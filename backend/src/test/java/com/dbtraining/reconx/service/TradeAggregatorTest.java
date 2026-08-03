package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * TICKET-ADV137 — Event sourcing rebuild
 */
@ExtendWith(MockitoExtension.class)
class TradeAggregatorTest {

    private static final String TRADE_REF = "TRD-20260315-0001";

    @Mock
    private AuditLogRepository auditRepo;

    private TradeAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new TradeAggregator(auditRepo, new ObjectMapper());
    }

    private AuditLogEntry entry(String eventId, String eventType, Instant ts, String after) {
        return new AuditLogEntry(eventId, TRADE_REF, eventType, ts, "trader@db.com", null, after);
    }

    @Test
    void rebuild_returnsEmpty_whenNoEventsExist() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc(TRADE_REF)).thenReturn(List.of());

        Optional<JsonNode> result = aggregator.rebuild(TRADE_REF);

        assertThat(result).isEmpty();
    }

    @Test
    void rebuild_foldsCreatedThenUpdated_intoTheLastAfterSnapshot() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc(TRADE_REF)).thenReturn(List.of(
                entry("evt-1", "TRADE_CREATED", Instant.parse("2026-03-15T09:00:00Z"), "{\"status\":\"PENDING\"}"),
                entry("evt-2", "TRADE_UPDATED", Instant.parse("2026-03-15T10:00:00Z"), "{\"status\":\"SETTLED\"}")
        ));

        Optional<JsonNode> result = aggregator.rebuild(TRADE_REF);

        assertThat(result).isPresent();
        assertThat(result.get().get("status").asText()).isEqualTo("SETTLED");
    }

    @Test
    void rebuild_returnsEmpty_whenTheLastEventIsACancellation() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc(TRADE_REF)).thenReturn(List.of(
                entry("evt-1", "TRADE_CREATED", Instant.parse("2026-03-15T09:00:00Z"), "{\"status\":\"PENDING\"}"),
                entry("evt-2", "TRADE_UPDATED", Instant.parse("2026-03-15T10:00:00Z"), "{\"status\":\"SETTLED\"}"),
                entry("evt-3", "TRADE_CANCELLED", Instant.parse("2026-03-15T11:00:00Z"), null)
        ));

        Optional<JsonNode> result = aggregator.rebuild(TRADE_REF);

        assertThat(result).isEmpty();
    }

    @Test
    void rebuild_revivesState_ifAnUpdateFollowsACancellation() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc(TRADE_REF)).thenReturn(List.of(
                entry("evt-1", "TRADE_CREATED", Instant.parse("2026-03-15T09:00:00Z"), "{\"status\":\"PENDING\"}"),
                entry("evt-2", "TRADE_CANCELLED", Instant.parse("2026-03-15T10:00:00Z"), null),
                entry("evt-3", "TRADE_UPDATED", Instant.parse("2026-03-15T11:00:00Z"), "{\"status\":\"REINSTATED\"}")
        ));

        Optional<JsonNode> result = aggregator.rebuild(TRADE_REF);

        assertThat(result).isPresent();
        assertThat(result.get().get("status").asText()).isEqualTo("REINSTATED");
    }
}
