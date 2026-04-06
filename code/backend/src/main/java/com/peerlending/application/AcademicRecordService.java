package com.peerlending.application;

import com.peerlending.api.dto.AcademicPendingRecordDto;
import com.peerlending.api.dto.AcademicRecordDto;
import com.peerlending.api.dto.RevokeVerifiedAcademicResultDto;
import com.peerlending.api.dto.SubmitAcademicRecordRequest;
import com.peerlending.common.exception.ForbiddenOperationException;
import com.peerlending.domain.ProfileVerificationStatus;
import com.peerlending.persistence.entity.AcademicRecordEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.AcademicRecordRepository;
import com.peerlending.persistence.repository.StudentProfileRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AcademicRecordService {

    private static final String AUTO_REJECT_REASON =
            "Автоматически снято с проверки: отправлена новая запись.";

    private final AcademicRecordRepository academicRecordRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public AcademicRecordService(
            AcademicRecordRepository academicRecordRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository
    ) {
        this.academicRecordRepository = academicRecordRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional
    public AcademicRecordDto submit(Long userId, SubmitAcademicRecordRequest request) {
        if (isUserBlocked(userId)) {
            throw new ForbiddenOperationException(
                    "Аккаунт заблокирован администратором. Подача успеваемости недоступна."
            );
        }
        cancelPendingSubmissionsForUser(userId);

        UserEntity user = userRepository.getReferenceById(userId);
        AcademicRecordEntity e = new AcademicRecordEntity();
        e.setUser(user);
        e.setGradeAverage(request.gradeAverage());
        e.setDescription(request.description());
        e.setVerified(false);
        e.setRejected(false);
        e = academicRecordRepository.save(e);

        studentProfileRepository.findById(userId).ifPresent(profile -> {
            profile.setVerificationStatus(ProfileVerificationStatus.PENDING);
            studentProfileRepository.save(profile);
        });

        return toDto(e);
    }

    /** Снимает с проверки все незакрытые заявки пользователя (перед новой подачей). */
    private void cancelPendingSubmissionsForUser(Long userId) {
        List<AcademicRecordEntity> pendings =
                academicRecordRepository.findByUser_IdAndVerifiedIsFalseAndRejectedIsFalseOrderBySubmittedAtDesc(userId);
        if (pendings.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (AcademicRecordEntity p : pendings) {
            p.setRejected(true);
            p.setRejectedAt(now);
            p.setRejectedBy(null);
            p.setRejectionReason(AUTO_REJECT_REASON);
            academicRecordRepository.save(p);
        }
        syncProfileVerificationStatus(userId);
    }

    /**
     * Статус успеваемости в профиле по факту всех записей: есть подтверждённая → VERIFIED, иначе есть
     * ожидающая → PENDING, иначе REJECTED. У заблокированного пользователя — BLOCKED.
     */
    private void syncProfileVerificationStatus(Long userId) {
        if (isUserBlocked(userId)) {
            studentProfileRepository.findById(userId).ifPresent(profile -> {
                profile.setVerificationStatus(ProfileVerificationStatus.BLOCKED);
                studentProfileRepository.save(profile);
            });
            return;
        }
        studentProfileRepository.findById(userId).ifPresent(profile -> {
            List<AcademicRecordEntity> all = academicRecordRepository.findByUser_IdOrderBySubmittedAtDesc(userId);
            boolean hasVerified = all.stream().anyMatch(AcademicRecordEntity::isVerified);
            if (hasVerified) {
                profile.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
            } else {
                boolean hasPending = all.stream().anyMatch(r -> !r.isVerified() && !r.isRejected());
                profile.setVerificationStatus(
                        hasPending ? ProfileVerificationStatus.PENDING : ProfileVerificationStatus.REJECTED);
            }
            studentProfileRepository.save(profile);
        });
    }

    /** После снятия блокировки пересчитать статус успеваемости по записям. */
    @Transactional
    public void syncProfileVerificationAfterUnblock(Long userId) {
        syncProfileVerificationStatus(userId);
    }

    private boolean isUserBlocked(Long userId) {
        return userRepository.findById(userId).map(UserEntity::isBlocked).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<AcademicRecordDto> listMine(Long userId) {
        return academicRecordRepository.findByUser_IdOrderBySubmittedAtDesc(userId).stream()
                .map(AcademicRecordService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicPendingRecordDto> listPendingForReview() {
        return academicRecordRepository.findByVerifiedIsFalseAndRejectedIsFalse().stream()
                .map(this::toPendingForReview)
                .toList();
    }

    private AcademicPendingRecordDto toPendingForReview(AcademicRecordEntity e) {
        UserEntity u = e.getUser();
        Long uid = u.getId();
        return studentProfileRepository.findById(uid)
                .map(profile -> new AcademicPendingRecordDto(
                        e.getId(),
                        e.getGradeAverage(),
                        e.getDescription(),
                        e.getSubmittedAt(),
                        uid,
                        u.getEmail(),
                        profile.getFullName(),
                        profile.getUniversity(),
                        profile.getStudentGroup(),
                        profile.getVerificationStatus().name()
                ))
                .orElseGet(() -> new AcademicPendingRecordDto(
                        e.getId(),
                        e.getGradeAverage(),
                        e.getDescription(),
                        e.getSubmittedAt(),
                        uid,
                        u.getEmail(),
                        "—",
                        null,
                        null,
                        "—"
                ));
    }

    @Transactional
    public AcademicRecordDto verify(Long recordId, Long adminUserId) {
        AcademicRecordEntity e = academicRecordRepository.findById(recordId)
                .orElseThrow(() -> new com.peerlending.common.exception.NotFoundException("Record not found"));
        if (e.isVerified()) {
            throw new com.peerlending.common.exception.ConflictException("Already verified");
        }
        if (e.isRejected()) {
            throw new com.peerlending.common.exception.ConflictException("Record was rejected");
        }
        UserEntity admin = userRepository.getReferenceById(adminUserId);
        e.setVerified(true);
        e.setVerifiedAt(Instant.now());
        e.setVerifiedBy(admin);
        academicRecordRepository.save(e);

        syncProfileVerificationStatus(e.getUser().getId());

        return toDto(e);
    }

    /**
     * Снимает подтверждение со всех проверенных записей успеваемости пользователя: помечает их отклонёнными,
     * пересчитывает статус профиля (без блокировки аккаунта).
     */
    @Transactional
    public RevokeVerifiedAcademicResultDto adminRevokeAllVerified(Long targetUserId, Long adminUserId, String reason) {
        UserEntity admin = userRepository.getReferenceById(adminUserId);
        String trimmed = reason != null ? reason.trim() : "";
        String suffix = " (подтверждение снято администратором)";
        String line = trimmed.isEmpty()
                ? "Подтверждение успеваемости снято администратором" + suffix
                : trimmed + suffix;

        List<AcademicRecordEntity> all = academicRecordRepository.findByUser_IdOrderBySubmittedAtDesc(targetUserId);
        Instant now = Instant.now();
        int count = 0;
        for (AcademicRecordEntity r : all) {
            if (!r.isVerified()) {
                continue;
            }
            r.setVerified(false);
            r.setVerifiedAt(null);
            r.setVerifiedBy(null);
            r.setRejected(true);
            r.setRejectedAt(now);
            r.setRejectedBy(admin);
            r.setRejectionReason(line);
            academicRecordRepository.save(r);
            count++;
        }
        syncProfileVerificationStatus(targetUserId);
        return new RevokeVerifiedAcademicResultDto(count);
    }

    @Transactional
    public AcademicRecordDto reject(Long recordId, Long adminUserId, String reason) {
        AcademicRecordEntity e = academicRecordRepository.findById(recordId)
                .orElseThrow(() -> new com.peerlending.common.exception.NotFoundException("Record not found"));
        if (e.isVerified()) {
            throw new com.peerlending.common.exception.ConflictException("Already verified");
        }
        if (e.isRejected()) {
            throw new com.peerlending.common.exception.ConflictException("Already rejected");
        }
        UserEntity admin = userRepository.getReferenceById(adminUserId);
        String trimmed = reason != null ? reason.trim() : "";
        e.setRejected(true);
        e.setRejectedAt(Instant.now());
        e.setRejectedBy(admin);
        e.setRejectionReason(trimmed.isEmpty() ? null : trimmed);
        academicRecordRepository.save(e);

        syncProfileVerificationStatus(e.getUser().getId());

        return toDto(e);
    }

    private static AcademicRecordDto toDto(AcademicRecordEntity e) {
        return new AcademicRecordDto(
                e.getId(),
                e.getGradeAverage(),
                e.getDescription(),
                e.getSubmittedAt(),
                e.isVerified(),
                e.getVerifiedAt(),
                e.isRejected(),
                e.getRejectedAt(),
                e.getRejectionReason()
        );
    }
}
