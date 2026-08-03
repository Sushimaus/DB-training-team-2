package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.dbtraining.reconx.security.JwtAuthenticationFilter;
import com.dbtraining.reconx.security.JwtTokenProvider;
import com.dbtraining.reconx.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DlqAdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DlqAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DlqMessageRepository repo;

    @MockBean
    private TradeEventProducer producer;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @WithMockUser(roles = "ADMIN")
    void replay_dryRunReturnsPreviewWithoutPublishing() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(repo.findByEventId(eventId.toString())).thenReturn(Optional.of(dlqMessage(eventId)));

        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .param("eventId", eventId.toString())
                        .param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.wouldReplayTo").value("trade-events"))
                .andExpect(jsonPath("$.tradeRef").value("TRD-20260315-0001"));

        verify(producer, never()).publish(any());
        verify(repo, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replay_republishesAndDeletesTheDlqRow() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(repo.findByEventId(eventId.toString())).thenReturn(Optional.of(dlqMessage(eventId)));

        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .param("eventId", eventId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.topic").value("trade-events"));

        verify(producer).publish(any(TradeEvent.class));
        verify(repo).delete(any(DlqMessage.class));
    }

    @Test
    void replay_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .param("eventId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void replay_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .param("eventId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replay_missingEventReturns404() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(repo.findByEventId(eventId.toString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .param("eventId", eventId.toString()))
                .andExpect(status().isNotFound());
    }

    private DlqMessage dlqMessage(UUID eventId) {
        return new DlqMessage(
                eventId.toString(),
                "TRD-20260315-0001",
                "trade-events",
                1,
                42L,
                TradeEvent.EventType.TRADE_CREATED.name(),
                Instant.parse("2026-03-15T09:00:00Z"),
                "trader@db.com",
                null,
                "{\"status\":\"PENDING\"}",
                "boom",
                Instant.parse("2026-03-15T09:01:00Z")
        );
    }
}
