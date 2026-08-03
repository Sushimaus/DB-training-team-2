package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * TICKET-ADV136 — DLQ consumer persistence.
 */
@ExtendWith(MockitoExtension.class)
class DlqConsumerTest {

    @Mock
    private DlqMessageRepository repo;

    @Test
    void onDlqMessage_savesTheFailedEventForReplay() {
        DlqConsumer consumer = new DlqConsumer(repo);
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-03-15T09:00:00Z");
        TradeEvent event = new TradeEvent(
                eventId,
                "TRD-20260315-0001",
                TradeEvent.EventType.TRADE_UPDATED,
                timestamp,
                "trader@db.com",
                "{\"status\":\"PENDING\"}",
                "{\"status\":\"BROKEN\"}"
        );
        ConsumerRecord<String, TradeEvent> record =
                new ConsumerRecord<>("trade-events-dlq", 2, 17L, event.tradeRef(), event);

        consumer.onDlqMessage(record, "boom");

        ArgumentCaptor<DlqMessage> captor = ArgumentCaptor.forClass(DlqMessage.class);
        verify(repo).save(captor.capture());
        DlqMessage saved = captor.getValue();

        assertThat(saved.getEventId()).isEqualTo(eventId.toString());
        assertThat(saved.getTradeRef()).isEqualTo(event.tradeRef());
        assertThat(saved.getOriginalTopic()).isEqualTo("trade-events");
        assertThat(saved.getPartitionNumber()).isEqualTo(2);
        assertThat(saved.getOffsetValue()).isEqualTo(17L);
        assertThat(saved.getEventType()).isEqualTo("TRADE_UPDATED");
        assertThat(saved.getEventTimestamp()).isEqualTo(timestamp);
        assertThat(saved.getActor()).isEqualTo("trader@db.com");
        assertThat(saved.getBeforeState()).isEqualTo("{\"status\":\"PENDING\"}");
        assertThat(saved.getAfterState()).isEqualTo("{\"status\":\"BROKEN\"}");
        assertThat(saved.getFailureReason()).isEqualTo("boom");
        assertThat(saved.getFirstSeen()).isNotNull();
    }

    @Test
    void onDlqMessage_isWiredToTheDlqTopicOnItsOwnConsumerGroup() throws NoSuchMethodException {
        Method method = DlqConsumer.class.getMethod(
                "onDlqMessage", ConsumerRecord.class, String.class);

        KafkaListener listener = method.getAnnotation(KafkaListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.topics()).containsExactly("trade-events-dlq");
        assertThat(listener.groupId()).isEqualTo("dlq-monitor");

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();

        Header header = method.getParameters()[1].getAnnotation(Header.class);
        assertThat(header).isNotNull();
        assertThat(header.value()).isEqualTo(KafkaHeaders.EXCEPTION_MESSAGE);
    }
}
