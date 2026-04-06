package com.peerlending.api.dto;

import java.util.List;

public record AdminAnalyticsOverviewDto(
        Totals totals,
        List<LabelCountDto> loanRequestsByStatus,
        List<LabelCountDto> loansByStatus,
        List<LabelCountDto> usersByRole,
        List<DailyCountDto> newUsersByDay,
        List<DailyCountDto> newLoanRequestsByDay
) {
    public record Totals(
            long users,
            long emailVerifiedUsers,
            long blockedUsers,
            long loanRequests,
            long loans
    ) {}

    public record LabelCountDto(String label, long count) {}

    public record DailyCountDto(String date, long count) {}
}
