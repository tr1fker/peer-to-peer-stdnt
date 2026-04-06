package com.peerlending.application.loan;

import com.peerlending.domain.InstallmentStatus;
import com.peerlending.persistence.entity.LoanEntity;
import com.peerlending.persistence.entity.PaymentInstallmentEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class InstallmentScheduleFactory {

    public List<PaymentInstallmentEntity> createEqualInstallments(LoanEntity loan) {
        int n = loan.getLoanRequest().getTermMonths();
        LocalDate start = loan.getStartDate();
        BigDecimal principal = loan.getPrincipal();
        BigDecimal annualRate = loan.getInterestRatePercent().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal years = BigDecimal.valueOf(n).divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        BigDecimal totalInterest = principal.multiply(annualRate).multiply(years).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = principal.add(totalInterest);
        BigDecimal monthly = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        List<PaymentInstallmentEntity> list = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 1; i <= n; i++) {
            PaymentInstallmentEntity inst = new PaymentInstallmentEntity();
            inst.setLoan(loan);
            inst.setInstallmentNumber(i);
            BigDecimal due = (i == n) ? total.subtract(allocated) : monthly;
            inst.setAmountDue(due);
            inst.setDueDate(start.plusMonths(i));
            inst.setStatus(InstallmentStatus.SCHEDULED);
            list.add(inst);
            allocated = allocated.add(due);
        }
        return list;
    }
}
