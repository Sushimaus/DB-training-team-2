package com.dbtraining.reconx.observability;

import com.dbtraining.reconx.repository.ReconBreakRepository;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TICKET-ADV085 — Gauge: recon_break_count
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
    }

    @Test
    @DisplayName("TICKET-ADV085: recon_break_count Gauge tracks open breaks from repository")
    void testReconBreakCountGauge() {
        when(breakRepo.countByStatus("OPEN")).thenReturn(5L);

        metrics = new TradeMetrics(registry, breakRepo);

        Gauge gauge = registry.find("recon_break_count").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.getId().getDescription()).isEqualTo("Open recon breaks");
        assertThat(gauge.value()).isEqualTo(5.0);

        when(breakRepo.countByStatus("OPEN")).thenReturn(4L);
        assertThat(gauge.value()).isEqualTo(4.0);

        verify(breakRepo, org.mockito.Mockito.atLeastOnce()).countByStatus("OPEN");
    }

    @Test
    void recordTradeValue_registersDistributionSummaryWithUsdBaseUnit() {
        metrics = new TradeMetrics(registry, breakRepo);
        DistributionSummary summary = registry.find("trade_value_total").summary();

        assertThat(summary).isNotNull();
        assertThat(summary.getId().getBaseUnit()).isEqualTo("USD");
    }

    @Test
    void recordTradeValue_recordsEachCallOnTheSummary() {
        metrics = new TradeMetrics(registry, breakRepo);
        metrics.recordTradeValue(24_550.0);
        metrics.recordTradeValue(5_000.0);

        DistributionSummary summary = registry.find("trade_value_total").summary();

        assertThat(summary.count()).isEqualTo(2);
        assertThat(summary.totalAmount()).isEqualTo(29_550.0);
    }
}
