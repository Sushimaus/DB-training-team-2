package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * TICKET-ADV136 — Consume failed trade events from the DLQ and persist them
 * for safe one-at-a-time replay by operators.
 */
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repo;

    public DlqConsumer(DlqMessageRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "trade-events-dlq", groupId = "dlq-monitor")
    @Transactional
    public void onDlqMessage(ConsumerRecord<String, TradeEvent> record,
                             @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exMsg) {
        TradeEvent event = record.value();

        repo.save(new DlqMessage(
                event.eventId().toString(),
                event.tradeRef(),
                record.topic().replace("-dlq", ""),
                record.partition(),
                record.offset(),
                event.eventType().name(),
                event.timestamp(),
                event.actor(),
                event.before(),
                event.after(),
                exMsg,
                Instant.now()
        ));

        log.error("DLQ event persisted eventId={} tradeRef={} partition={} offset={} reason={}",
                event.eventId(), event.tradeRef(), record.partition(), record.offset(), exMsg);
    }
}
