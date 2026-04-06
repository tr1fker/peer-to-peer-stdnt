package com.peerlending.application;

import com.peerlending.application.scoring.CreditLimitStrategy;
import com.peerlending.persistence.entity.AcademicRecordEntity;
import com.peerlending.persistence.repository.AcademicRecordRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CreditLimitService {

    private final AcademicRecordRepository academicRecordRepository;
    private final CreditLimitStrategy creditLimitStrategy;
    private final UserRepository userRepository;

    public CreditLimitService(
            AcademicRecordRepository academicRecordRepository,
            CreditLimitStrategy creditLimitStrategy,
            UserRepository userRepository
    ) {
        this.academicRecordRepository = academicRecordRepository;
        this.creditLimitStrategy = creditLimitStrategy;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal maxLoanAmountForBorrower(Long userId) {
        if (isUserBlocked(userId)) {
            return BigDecimal.ZERO;
        }
        List<AcademicRecordEntity> records = academicRecordRepository.findByUser_IdOrderBySubmittedAtDesc(userId);
        Optional<BigDecimal> bestVerifiedGrade = records.stream()
                .filter(AcademicRecordEntity::isVerified)
                .map(AcademicRecordEntity::getGradeAverage)
                .filter(ga -> ga != null)
                .max(Comparator.naturalOrder());
        return creditLimitStrategy.maxLoanAmount(bestVerifiedGrade);
    }

    /** At least one academic row verified by admin — required to open loan requests. */
    @Transactional(readOnly = true)
    public boolean hasVerifiedAcademicRecord(Long userId) {
        if (isUserBlocked(userId)) {
            return false;
        }
        return academicRecordRepository.findByUser_IdOrderBySubmittedAtDesc(userId).stream()
                .anyMatch(AcademicRecordEntity::isVerified);
    }

    private boolean isUserBlocked(Long userId) {
        return userRepository.findById(userId).map(u -> u.isBlocked()).orElse(false);
    }
}
