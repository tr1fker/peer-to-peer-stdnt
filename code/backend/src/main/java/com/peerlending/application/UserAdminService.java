package com.peerlending.application;

import com.peerlending.api.dto.AdminUserDetailDto;
import com.peerlending.api.dto.AdminUserRowDto;
import com.peerlending.api.dto.RevokeVerifiedAcademicResultDto;
import com.peerlending.common.exception.ConflictException;
import com.peerlending.common.exception.ForbiddenOperationException;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.domain.LoanStatus;
import com.peerlending.domain.ProfileVerificationStatus;
import com.peerlending.persistence.entity.AcademicRecordEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.AcademicRecordRepository;
import com.peerlending.persistence.repository.LoanRepository;
import com.peerlending.persistence.repository.LoanRequestRepository;
import com.peerlending.persistence.repository.StudentProfileRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private static final String BLOCK_ACADEMIC_SUFFIX = " (блокировка аккаунта администратором)";

    private final UserRepository userRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanRepository loanRepository;
    private final AcademicRecordService academicRecordService;

    public UserAdminService(
            UserRepository userRepository,
            AcademicRecordRepository academicRecordRepository,
            StudentProfileRepository studentProfileRepository,
            LoanRequestRepository loanRequestRepository,
            LoanRepository loanRepository,
            AcademicRecordService academicRecordService
    ) {
        this.userRepository = userRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.loanRequestRepository = loanRequestRepository;
        this.loanRepository = loanRepository;
        this.academicRecordService = academicRecordService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserRowDto> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(UserEntity::getId))
                .map(this::toAdminRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(Long userId) {
        UserEntity u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toAdminDetail(u);
    }

    @Transactional
    public RevokeVerifiedAcademicResultDto revokeVerifiedAcademic(Long targetUserId, Long adminUserId, String reason) {
        if (targetUserId.equals(adminUserId)) {
            throw new ForbiddenOperationException("Некорректная операция");
        }
        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (isRoleAdmin(target)) {
            throw new ForbiddenOperationException("Нельзя применять к администратору");
        }
        return academicRecordService.adminRevokeAllVerified(targetUserId, adminUserId, reason);
    }

    private static boolean isRoleAdmin(UserEntity u) {
        return u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
    }

    private AdminUserDetailDto toAdminDetail(UserEntity u) {
        Long id = u.getId();
        Set<String> roles = u.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());
        return studentProfileRepository.findById(id)
                .map(profile -> new AdminUserDetailDto(
                        id,
                        u.getEmail(),
                        u.isEnabled(),
                        u.isEmailVerified(),
                        u.getCreatedAt(),
                        u.isBlocked(),
                        u.getBlockedReason(),
                        u.getBlockedAt(),
                        roles,
                        profile.getFullName(),
                        profile.getUniversity(),
                        profile.getStudentGroup(),
                        profile.getVerificationStatus().name(),
                        academicRecordRepository.countByUser_IdAndVerifiedIsTrue(id),
                        academicRecordRepository.countByUser_IdAndVerifiedIsFalseAndRejectedIsFalse(id),
                        loanRequestRepository.countByBorrower_IdAndStatus(id, LoanRequestStatus.OPEN),
                        loanRepository.countByBorrowerIdAndStatus(id, LoanStatus.ACTIVE)
                ))
                .orElseGet(() -> new AdminUserDetailDto(
                        id,
                        u.getEmail(),
                        u.isEnabled(),
                        u.isEmailVerified(),
                        u.getCreatedAt(),
                        u.isBlocked(),
                        u.getBlockedReason(),
                        u.getBlockedAt(),
                        roles,
                        "—",
                        null,
                        null,
                        "PENDING",
                        academicRecordRepository.countByUser_IdAndVerifiedIsTrue(id),
                        academicRecordRepository.countByUser_IdAndVerifiedIsFalseAndRejectedIsFalse(id),
                        loanRequestRepository.countByBorrower_IdAndStatus(id, LoanRequestStatus.OPEN),
                        loanRepository.countByBorrowerIdAndStatus(id, LoanStatus.ACTIVE)
                ));
    }

    private AdminUserRowDto toAdminRow(UserEntity u) {
        Set<String> roles = u.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());
        String fullName = studentProfileRepository.findById(u.getId())
                .map(p -> p.getFullName())
                .orElse("—");
        return new AdminUserRowDto(
                u.getId(),
                u.getEmail(),
                u.isEnabled(),
                u.isBlocked(),
                u.getBlockedReason(),
                u.getBlockedAt(),
                roles,
                fullName
        );
    }

    @Transactional
    public void blockUser(Long targetUserId, Long adminUserId, String reason) {
        if (targetUserId.equals(adminUserId)) {
            throw new ForbiddenOperationException("Нельзя заблокировать собственный аккаунт");
        }
        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (target.isBlocked()) {
            throw new ConflictException("Пользователь уже заблокирован");
        }
        if (target.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            throw new ForbiddenOperationException("Нельзя заблокировать администратора");
        }

        UserEntity admin = userRepository.getReferenceById(adminUserId);
        String trimmed = reason != null ? reason.trim() : "";
        String reasonLine = trimmed.isEmpty()
                ? "Блокировка администратором" + BLOCK_ACADEMIC_SUFFIX
                : trimmed + BLOCK_ACADEMIC_SUFFIX;

        revokeOpenAcademicRecords(targetUserId, admin, reasonLine);

        target.setBlocked(true);
        target.setBlockedReason(trimmed.isEmpty() ? null : trimmed);
        target.setBlockedAt(Instant.now());
        target.setBlockedBy(admin);
        userRepository.save(target);

        studentProfileRepository.findById(targetUserId).ifPresent(profile -> {
            profile.setVerificationStatus(ProfileVerificationStatus.BLOCKED);
            studentProfileRepository.save(profile);
        });
    }

    private void revokeOpenAcademicRecords(Long userId, UserEntity admin, String rejectionReason) {
        List<AcademicRecordEntity> records = academicRecordRepository.findByUser_IdOrderBySubmittedAtDesc(userId);
        Instant now = Instant.now();
        for (AcademicRecordEntity r : records) {
            if (r.isRejected()) {
                continue;
            }
            if (r.isVerified()) {
                r.setVerified(false);
                r.setVerifiedAt(null);
                r.setVerifiedBy(null);
            }
            r.setRejected(true);
            r.setRejectedAt(now);
            r.setRejectedBy(admin);
            r.setRejectionReason(rejectionReason);
            academicRecordRepository.save(r);
        }
    }

    @Transactional
    public void unblockUser(Long targetUserId, Long adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            throw new ForbiddenOperationException("Некорректная операция");
        }
        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!target.isBlocked()) {
            throw new ConflictException("Пользователь не заблокирован");
        }
        target.setBlocked(false);
        target.setBlockedReason(null);
        target.setBlockedAt(null);
        target.setBlockedBy(null);
        userRepository.save(target);

        academicRecordService.syncProfileVerificationAfterUnblock(targetUserId);
    }
}
