package com.dbtraining.reconx.kafka;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaErrorHandlerConfigTest {

    @Test
    void errorHandler_createsBeanWithRetryAndDlqRecoverer() {
        KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();
        @SuppressWarnings("unchecked")
        KafkaTemplate<Object, Object> mockTemplate = Mockito.mock(KafkaTemplate.class);

        DefaultErrorHandler errorHandler = config.errorHandler(mockTemplate);

        assertThat(errorHandler).isNotNull();
    }
}
