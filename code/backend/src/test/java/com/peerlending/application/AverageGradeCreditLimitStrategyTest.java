package com.peerlending.application;

import com.peerlending.application.scoring.AverageGradeCreditLimitStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AverageGradeCreditLimitStrategyTest {

    private final AverageGradeCreditLimitStrategy strategy = new AverageGradeCreditLimitStrategy();

    @Test
    void topGradeGivesHighestLimit() {
        assertEquals(new BigDecimal("200000"), strategy.maxLoanAmount(Optional.of(new BigDecimal("9.5"))));
    }

    @Test
    void midGradeGivesMidLimit() {
        assertEquals(new BigDecimal("120000"), strategy.maxLoanAmount(Optional.of(new BigDecimal("8.5"))));
    }

    @Test
    void lowVerifiedGradeGivesFloorLimit() {
        assertEquals(new BigDecimal("20000"), strategy.maxLoanAmount(Optional.of(new BigDecimal("5.0"))));
    }

    @Test
    void noGradeUsesFloorLimit() {
        assertEquals(new BigDecimal("20000"), strategy.maxLoanAmount(Optional.empty()));
    }
}
