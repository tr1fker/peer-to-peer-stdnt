package com.peerlending.application;

import com.peerlending.api.dto.LoanDto;
import com.peerlending.api.dto.LoanRequestDto;
import com.peerlending.api.dto.MyInvestmentDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PortfolioExportService {

    private final LoanRequestService loanRequestService;
    private final LoanQueryService loanQueryService;

    public PortfolioExportService(LoanRequestService loanRequestService, LoanQueryService loanQueryService) {
        this.loanRequestService = loanRequestService;
        this.loanQueryService = loanQueryService;
    }

    @Transactional(readOnly = true)
    public String buildCsv(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        csvLine(sb, "section", "id", "loanRequestId", "amountOrPrincipal", "status", "extra1", "extra2", "extra3");

        List<LoanRequestDto> myRequests = loanRequestService.listForBorrower(userId);
        for (LoanRequestDto r : myRequests) {
            csvLine(
                    sb,
                    "my_request",
                    String.valueOf(r.id()),
                    "",
                    r.amount().toPlainString(),
                    r.status().name(),
                    String.valueOf(r.termMonths()),
                    r.interestRatePercent().toPlainString(),
                    r.fundedAmount().toPlainString()
            );
        }

        List<LoanDto> borrowed = loanQueryService.myLoansAsBorrower(userId);
        for (LoanDto l : borrowed) {
            csvLine(
                    sb,
                    "loan_borrower",
                    String.valueOf(l.id()),
                    String.valueOf(l.loanRequestId()),
                    l.principal().toPlainString(),
                    l.status().name(),
                    l.startDate().toString(),
                    l.endDate().toString(),
                    l.interestRatePercent().toPlainString()
            );
        }

        List<MyInvestmentDto> investments = loanQueryService.myInvestments(userId);
        for (MyInvestmentDto inv : investments) {
            String loanId = inv.loanId() != null ? String.valueOf(inv.loanId()) : "";
            csvLine(
                    sb,
                    "investment",
                    String.valueOf(inv.investmentId()),
                    String.valueOf(inv.loanRequestId()),
                    inv.amount().toPlainString(),
                    inv.loanRequestStatus().name(),
                    inv.loanRequestAmount().toPlainString(),
                    inv.fundedAmount().toPlainString(),
                    loanId
            );
        }

        return sb.toString();
    }

    private static void csvLine(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escape(cells[i]));
        }
        sb.append('\n');
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        boolean needQuote = raw.indexOf(';') >= 0 || raw.indexOf('"') >= 0 || raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0;
        String s = raw.replace("\"", "\"\"");
        return needQuote ? "\"" + s + "\"" : s;
    }
}
