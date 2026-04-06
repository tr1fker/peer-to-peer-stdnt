package com.peerlending.application;

import com.peerlending.api.dto.AdminAnalyticsOverviewDto;
import com.peerlending.api.dto.AdminAnalyticsOverviewDto.DailyCountDto;
import com.peerlending.api.dto.AdminAnalyticsOverviewDto.LabelCountDto;
import com.peerlending.api.dto.AdminAnalyticsOverviewDto.Totals;
import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.domain.LoanStatus;
import com.peerlending.persistence.repository.LoanRepository;
import com.peerlending.persistence.repository.LoanRequestRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAnalyticsService {

    private static final int DAILY_WINDOW_DAYS = 14;

    private final UserRepository userRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanRepository loanRepository;

    public AdminAnalyticsService(
            UserRepository userRepository,
            LoanRequestRepository loanRequestRepository,
            LoanRepository loanRepository
    ) {
        this.userRepository = userRepository;
        this.loanRequestRepository = loanRequestRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewDto overview() {
        Instant from = Instant.now().minus(DAILY_WINDOW_DAYS, ChronoUnit.DAYS);

        Totals totals = new Totals(
                userRepository.count(),
                userRepository.countByEmailVerified(true),
                userRepository.countByBlocked(true),
                loanRequestRepository.count(),
                loanRepository.count()
        );

        List<LabelCountDto> loanRequestsByStatus = new ArrayList<>();
        for (LoanRequestStatus s : LoanRequestStatus.values()) {
            loanRequestsByStatus.add(new LabelCountDto(loanRequestStatusRu(s), loanRequestRepository.countByStatus(s)));
        }

        List<LabelCountDto> loansByStatus = new ArrayList<>();
        for (LoanStatus s : LoanStatus.values()) {
            loansByStatus.add(new LabelCountDto(loanStatusRu(s), loanRepository.countByStatus(s)));
        }

        List<LabelCountDto> usersByRole = userRepository.countDistinctUsersPerRoleName().stream()
                .map(row -> new LabelCountDto(roleRu(String.valueOf(row[0])), ((Number) row[1]).longValue()))
                .toList();

        List<DailyCountDto> newUsersByDay = fillDailySeries(from, userRepository.countNewUsersByDayUtc(from));
        List<DailyCountDto> newLoanRequestsByDay = fillDailySeries(from, loanRequestRepository.countNewLoanRequestsByDayUtc(from));

        return new AdminAnalyticsOverviewDto(
                totals,
                loanRequestsByStatus,
                loansByStatus,
                usersByRole,
                newUsersByDay,
                newLoanRequestsByDay
        );
    }

    private static List<DailyCountDto> fillDailySeries(Instant from, List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate d = toLocalDate(row[0]);
            map.put(d, ((Number) row[1]).longValue());
        }
        LocalDate start = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        List<DailyCountDto> out = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            out.add(new DailyCountDto(d.toString(), map.getOrDefault(d, 0L)));
        }
        return out;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) {
            return d;
        }
        if (o instanceof Date sql) {
            return sql.toLocalDate();
        }
        if (o instanceof java.util.Date u) {
            return u.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (o instanceof Instant i) {
            return i.atZone(ZoneOffset.UTC).toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported date type: " + (o == null ? "null" : o.getClass()));
    }

    private static String loanRequestStatusRu(LoanRequestStatus s) {
        return switch (s) {
            case OPEN -> "Открыта";
            case FUNDED -> "Профинансирована";
            case CANCELLED -> "Отменена";
        };
    }

    private static String loanStatusRu(LoanStatus s) {
        return switch (s) {
            case ACTIVE -> "Активен";
            case COMPLETED -> "Закрыт";
            case DEFAULTED -> "Дефолт";
        };
    }

    private static String roleRu(String roleName) {
        String n = roleName == null ? "" : roleName.trim();
        return switch (n) {
            case "ROLE_BORROWER" -> "Заёмщик";
            case "ROLE_LENDER" -> "Инвестор";
            case "ROLE_ADMIN" -> "Администратор";
            default -> n.startsWith("ROLE_") ? n.substring(5) : n;
        };
    }
}
