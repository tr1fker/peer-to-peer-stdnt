package com.peerlending.persistence.repository;

import com.peerlending.persistence.entity.AcademicRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecordEntity, Long> {

    List<AcademicRecordEntity> findByUser_IdOrderBySubmittedAtDesc(Long userId);

    long countByUser_IdAndVerifiedIsTrue(Long userId);

    long countByUser_IdAndVerifiedIsFalseAndRejectedIsFalse(Long userId);

    List<AcademicRecordEntity> findByVerifiedIsFalseAndRejectedIsFalse();

    /** Ожидают проверки по конкретному пользователю (для отмены при новой подаче). */
    List<AcademicRecordEntity> findByUser_IdAndVerifiedIsFalseAndRejectedIsFalseOrderBySubmittedAtDesc(Long userId);
}
