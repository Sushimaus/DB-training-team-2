package com.dbtraining.reconx.observability;

import com.dbtraining.reconx.repository.ReconBreakRepository;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV086 — DistributionSummary: trade_value_total
 */
@ExtendWith(MockitoExtension.class)
class TradeMetricsTest {

    @Mock
    private ReconBreakRepository breakRepo;

    private SimpleMeterRegistry registry;
    private TradeMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new TradeMetrics(registry, breakRepo);
    }

    @Test
    void recordTradeValue_registersDistributionSummaryWithUsdBaseUnit() {
        DistributionSummary summary = registry.find("trade_value_total").summary();

        assertThat(summary).isNotNull();
        assertThat(summary.getId().getBaseUnit()).isEqualTo("USD");
    }

    @Test
    void recordTradeValue_recordsEachCallOnTheSummary() {
        metrics.recordTradeValue(24_550.0);
        metrics.recordTradeValue(5_000.0);

        DistributionSummary summary = registry.find("trade_value_total").summary();

        assertThat(summary.count()).isEqualTo(2);
        assertThat(summary.totalAmount()).isEqualTo(29_550.0);
    }
}
