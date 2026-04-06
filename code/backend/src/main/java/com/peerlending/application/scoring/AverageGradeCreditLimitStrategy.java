package com.peerlending.application.scoring;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component("averageGradeCreditLimitStrategy")
public class AverageGradeCreditLimitStrategy implements CreditLimitStrategy {

    /** Пороги дублируются в UI (компонент CreditLimitTiersHint во фронтенде) — менять в двух местах. */
    @Override
    public BigDecimal maxLoanAmount(Optional<BigDecimal> verifiedGradeAverage) {
        /* Пороги под 10-балльную шкалу (РБ): верхний / средний / базовый тир лимита */
        return verifiedGradeAverage
                .filter(ga -> ga.compareTo(BigDecimal.valueOf(9.0)) >= 0)
                .map(ga -> BigDecimal.valueOf(200_000))
                .or(() -> verifiedGradeAverage
                        .filter(ga -> ga.compareTo(BigDecimal.valueOf(8.0)) >= 0)
                        .map(ga -> BigDecimal.valueOf(120_000)))
                .or(() -> verifiedGradeAverage
                        .filter(ga -> ga.compareTo(BigDecimal.valueOf(6.0)) >= 0)
                        .map(ga -> BigDecimal.valueOf(60_000)))
                .orElse(BigDecimal.valueOf(20_000));
    }
}
