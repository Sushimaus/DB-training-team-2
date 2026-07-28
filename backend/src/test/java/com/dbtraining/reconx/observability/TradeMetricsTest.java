package com.dbtraining.reconx.observability;

import com.dbtraining.reconx.repository.ReconBreakRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeMetricsTest {

    private MeterRegistry registry;
    private ReconBreakRepository breakRepo;
    private TradeMetrics tradeMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        breakRepo = mock(ReconBreakRepository.class);
    }

    @Test
    @DisplayName("TICKET-ADV085: recon_break_count Gauge tracks open breaks from repository")
    void testReconBreakCountGauge() {
        when(breakRepo.countByStatus("OPEN")).thenReturn(5L);

        tradeMetrics = new TradeMetrics(registry, breakRepo);

        Gauge gauge = registry.find("recon_break_count").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.getId().getDescription()).isEqualTo("Open recon breaks");
        assertThat(gauge.value()).isEqualTo(5.0);

        when(breakRepo.countByStatus("OPEN")).thenReturn(4L);
        assertThat(gauge.value()).isEqualTo(4.0);

        verify(breakRepo, org.mockito.Mockito.atLeastOnce()).countByStatus("OPEN");
    }
}
