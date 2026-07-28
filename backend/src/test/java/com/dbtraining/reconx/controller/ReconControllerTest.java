package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReconControllerTest {

    private ReconBreakRepository reconBreakRepository;
    private ReconController reconController;

    @BeforeEach
    void setUp() {
        reconBreakRepository = Mockito.mock(ReconBreakRepository.class);
        reconController = new ReconController(reconBreakRepository);
    }

    @Test
    void results_returnsAllBreaksFromRepository() {
        ReconBreak reconBreak = new ReconBreak();
        reconBreak.setTradeId(1L);
        reconBreak.setDiscrepancyType("PRICE_MISMATCH");

        when(reconBreakRepository.findAll()).thenReturn(List.of(reconBreak));

        List<ReconBreak> results = reconController.results("job-123");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTradeId()).isEqualTo(1L);
        assertThat(results.get(0).getDiscrepancyType()).isEqualTo("PRICE_MISMATCH");
        verify(reconBreakRepository).findAll();
    }
}
