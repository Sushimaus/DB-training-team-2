package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * TICKET-ADV136 — Admin-only DLQ replay endpoint.
 */
@RestController
@RequestMapping("/v1/admin/dlq")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "dlq-admin")
@SecurityRequirement(name = "bearerAuth")
public class DlqAdminController {

    private final DlqMessageRepository repo;
    private final TradeEventProducer producer;

    public DlqAdminController(DlqMessageRepository repo, TradeEventProducer producer) {
        this.repo = repo;
        this.producer = producer;
    }

    @PostMapping("/replay")
    @Operation(summary = "Replay one failed trade event from the DLQ")
    public ResponseEntity<Map<String, Object>> replay(@RequestParam UUID eventId,
                                                      @RequestParam(defaultValue = "false") boolean dryRun) {
        DlqMessage msg = repo.findByEventId(eventId.toString())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "No DLQ message found for eventId=" + eventId));

        if (dryRun) {
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("dryRun", true);
            preview.put("eventId", msg.getEventId());
            preview.put("wouldReplayTo", msg.getOriginalTopic());
            preview.put("tradeRef", msg.getTradeRef());
            preview.put("reason", msg.getFailureReason());
            return ResponseEntity.ok(preview);
        }

        producer.publish(msg.toTradeEvent());
        repo.delete(msg);

        return ResponseEntity.ok(Map.of(
                "replayed", true,
                "eventId", msg.getEventId(),
                "topic", msg.getOriginalTopic()
        ));
    }
}
