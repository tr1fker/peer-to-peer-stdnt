package com.peerlending.application.scoring;

import java.math.BigDecimal;
import java.util.Optional;

@FunctionalInterface
public interface CreditLimitStrategy {

    BigDecimal maxLoanAmount(Optional<BigDecimal> verifiedGradeAverage);
}
