package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * TICKET-ADV137 — Event sourcing rebuild
 *
 * WHAT:    Folds a trade's full audit_log event history back into its
 *          current state, proving the event log alone is enough to
 *          reconstruct any trade.
 * HOW:     Reads AuditLogEntry rows oldest-first and replays them: CREATED
 *          and UPDATED adopt the event's after-state, CANCELLED clears it.
 * WHY:     Together with TICKET-ADV132 (the consumer that writes audit_log)
 *          this is the event-sourcing pair — the table is a projection, the
 *          event log is the source of truth.
 * OBSERVE: created -> updated -> cancelled replays to Optional.empty();
 *          without the cancellation it returns the last UPDATED snapshot.
 * ============================================================================
 */
@Service
public class TradeAggregator {

    private final AuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;

    public TradeAggregator(AuditLogRepository auditRepo, ObjectMapper objectMapper) {
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> rebuild(String tradeRef) {
        List<AuditLogEntry> events = auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        JsonNode state = null;
        for (AuditLogEntry e : events) {
            switch (TradeEvent.EventType.valueOf(e.getEventType())) {
                case TRADE_CREATED, TRADE_UPDATED -> state = readJson(e.getAfterState());
                case TRADE_CANCELLED -> state = null;
            }
        }
        return Optional.ofNullable(state);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupt audit_log after-state JSON", ex);
        }
    }
}
