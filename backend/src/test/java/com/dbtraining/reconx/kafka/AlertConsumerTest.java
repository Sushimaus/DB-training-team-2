package com.dbtraining.reconx.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TICKET-ADV133 — AlertConsumerTest
 */
class AlertConsumerTest {

    @Test
    void onAlert_logsPayloadWithoutThrowingException() {
        AlertConsumer consumer = new AlertConsumer();
        assertThatCode(() -> consumer.onAlert("High CPU Usage"))
                .doesNotThrowAnyException();
    }

    @Test
    void onAlert_isWiredToSystemAlertsTopicWithAlertServiceConsumerGroup() throws NoSuchMethodException {
        Method method = AlertConsumer.class.getMethod("onAlert", String.class);

        KafkaListener listener = method.getAnnotation(KafkaListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.topics()).containsExactly("system-alerts");
        assertThat(listener.groupId()).isEqualTo("alert-service");
    }
}
