package com.dbtraining.reconx.repository.entity;

import com.dbtraining.reconx.dto.TradeEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET-ADV136 — Persisted copy of a failed Kafka message for admin replay.
 */
@Entity
@Table(name = "dlq_messages")
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "partition_number", nullable = false)
    private Integer partitionNumber;

    @Column(name = "offset_value", nullable = false)
    private Long offsetValue;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(length = 100)
    private String actor;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    public DlqMessage() {
    }

    public DlqMessage(String eventId,
                      String tradeRef,
                      String originalTopic,
                      Integer partitionNumber,
                      Long offsetValue,
                      String eventType,
                      Instant eventTimestamp,
                      String actor,
                      String beforeState,
                      String afterState,
                      String failureReason,
                      Instant firstSeen) {
        this.eventId = eventId;
        this.tradeRef = tradeRef;
        this.originalTopic = originalTopic;
        this.partitionNumber = partitionNumber;
        this.offsetValue = offsetValue;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.actor = actor;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.failureReason = failureReason;
        this.firstSeen = firstSeen;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTradeRef() {
        return tradeRef;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public Integer getPartitionNumber() {
        return partitionNumber;
    }

    public Long getOffsetValue() {
        return offsetValue;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public String getActor() {
        return actor;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public TradeEvent toTradeEvent() {
        return new TradeEvent(
                UUID.fromString(eventId),
                tradeRef,
                TradeEvent.EventType.valueOf(eventType),
                eventTimestamp,
                actor,
                beforeState,
                afterState
        );
    }
}
