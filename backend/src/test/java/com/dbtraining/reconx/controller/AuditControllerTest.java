package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TICKET-ADV071 — GET /api/v1/audit/trades/{tradeRef}
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditLogRepository auditRepo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditController(auditRepo)).build();
    }

    @Test
    void history_returnsRevisionsOldestFirst() throws Exception {
        String tradeRef = "TRD-20260315-0001";
        AuditLogEntry created = new AuditLogEntry(
                "evt-1", tradeRef, "ADD",
                Instant.parse("2026-03-15T09:00:00Z"), "trader@db.com",
                null, "{\"status\":\"PENDING\"}");
        AuditLogEntry amended = new AuditLogEntry(
                "evt-2", tradeRef, "MOD",
                Instant.parse("2026-03-15T10:00:00Z"), "admin@db.com",
                "{\"status\":\"PENDING\"}", "{\"status\":\"SETTLED\"}");

        when(auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef))
                .thenReturn(List.of(created, amended));

        mockMvc.perform(get("/v1/audit/trades/{tradeRef}", tradeRef))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventType").value("ADD"))
                .andExpect(jsonPath("$[0].actor").value("trader@db.com"))
                .andExpect(jsonPath("$[1].eventType").value("MOD"))
                .andExpect(jsonPath("$[1].actor").value("admin@db.com"));

        verify(auditRepo).findByTradeRefOrderByEventTimestampAsc(eq(tradeRef));
    }

    @Test
    void history_noRevisions_returnsEmptyArray() throws Exception {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-UNKNOWN"))
                .thenReturn(List.of());

        mockMvc.perform(get("/v1/audit/trades/{tradeRef}", "TRD-UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
