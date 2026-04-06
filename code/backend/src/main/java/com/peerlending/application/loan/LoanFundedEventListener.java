package com.peerlending.application.loan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LoanFundedEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoanFundedEventListener.class);

    @EventListener
    public void onFunded(LoanFundedEvent event) {
        log.info("Loan request {} converted to loan {}", event.loanRequestId(), event.loanId());
    }
}
