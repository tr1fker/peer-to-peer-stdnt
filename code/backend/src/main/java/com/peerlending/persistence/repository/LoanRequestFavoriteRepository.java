package com.peerlending.persistence.repository;

import com.peerlending.persistence.entity.LoanRequestFavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRequestFavoriteRepository extends JpaRepository<LoanRequestFavoriteEntity, Long> {

    Optional<LoanRequestFavoriteEntity> findByUser_IdAndLoanRequest_Id(Long userId, Long loanRequestId);

    List<LoanRequestFavoriteEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);

    void deleteByUser_IdAndLoanRequest_Id(Long userId, Long loanRequestId);

    boolean existsByUser_IdAndLoanRequest_Id(Long userId, Long loanRequestId);
}
