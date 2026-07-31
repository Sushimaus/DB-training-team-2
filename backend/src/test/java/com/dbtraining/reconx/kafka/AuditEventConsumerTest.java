package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * TICKET-ADV132 — AuditEventConsumer
 */
@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditLogRepository repo;

    @Test
    void onTradeEvent_savesAnAuditLogEntryMappedFromTheEvent() {
        AuditEventConsumer consumer = new AuditEventConsumer(repo);
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-03-15T09:00:00Z");
        TradeEvent event = new TradeEvent(
                eventId, "TRD-20260315-0001", TradeEvent.EventType.TRADE_CREATED,
                timestamp, "trader@db.com", null, "{\"status\":\"PENDING\"}");

        consumer.onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repo).save(captor.capture());
        AuditLogEntry saved = captor.getValue();

        assertThat(saved.getEventId()).isEqualTo(eventId.toString());
        assertThat(saved.getTradeRef()).isEqualTo("TRD-20260315-0001");
        assertThat(saved.getEventType()).isEqualTo("TRADE_CREATED");
        assertThat(saved.getEventTimestamp()).isEqualTo(timestamp);
        assertThat(saved.getActor()).isEqualTo("trader@db.com");
        assertThat(saved.getBeforeState()).isNull();
        assertThat(saved.getAfterState()).isEqualTo("{\"status\":\"PENDING\"}");
    }

    @Test
    void onTradeEvent_isWiredToTheTradeEventsTopicOnItsOwnConsumerGroup() throws NoSuchMethodException {
        Method method = AuditEventConsumer.class.getMethod("onTradeEvent", TradeEvent.class);

        KafkaListener listener = method.getAnnotation(KafkaListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.topics()).containsExactly("trade-events");
        assertThat(listener.groupId()).isEqualTo("audit-service");

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }
}
