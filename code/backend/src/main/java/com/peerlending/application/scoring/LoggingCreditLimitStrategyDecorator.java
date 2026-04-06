package com.peerlending.application.scoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Primary
public class LoggingCreditLimitStrategyDecorator implements CreditLimitStrategy {

    private static final Logger log = LoggerFactory.getLogger(LoggingCreditLimitStrategyDecorator.class);

    private final CreditLimitStrategy delegate;

    public LoggingCreditLimitStrategyDecorator(
            @Qualifier("averageGradeCreditLimitStrategy") CreditLimitStrategy delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    public BigDecimal maxLoanAmount(Optional<BigDecimal> verifiedGradeAverage) {
        BigDecimal limit = delegate.maxLoanAmount(verifiedGradeAverage);
        log.debug("Computed credit limit {} for grade {}", limit, verifiedGradeAverage.map(BigDecimal::toPlainString).orElse("none"));
        return limit;
    }
}
