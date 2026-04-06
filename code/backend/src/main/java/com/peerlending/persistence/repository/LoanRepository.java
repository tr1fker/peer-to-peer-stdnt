package com.peerlending.persistence.repository;

import com.peerlending.domain.LoanStatus;
import com.peerlending.persistence.entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    long countByStatus(LoanStatus status);

    Optional<LoanEntity> findByLoanRequest_Id(Long loanRequestId);

    List<LoanEntity> findByLoanRequest_Borrower_Id(Long borrowerId);

    @Query("SELECT COUNT(l) FROM LoanEntity l WHERE l.loanRequest.borrower.id = :borrowerId AND l.status = :status")
    long countByBorrowerIdAndStatus(@Param("borrowerId") Long borrowerId, @Param("status") LoanStatus status);
}
