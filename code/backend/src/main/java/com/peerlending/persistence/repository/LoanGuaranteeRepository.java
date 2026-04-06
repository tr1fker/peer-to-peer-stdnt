package com.peerlending.persistence.repository;

import com.peerlending.domain.GuaranteeType;
import com.peerlending.persistence.entity.LoanGuaranteeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoanGuaranteeRepository extends JpaRepository<LoanGuaranteeEntity, Long> {

    List<LoanGuaranteeEntity> findByLoanId(Long loanId);

    @Query("""
            SELECT DISTINCT g FROM LoanGuaranteeEntity g
            JOIN FETCH g.loan l
            JOIN FETCH l.loanRequest lr
            JOIN FETCH lr.borrower b
            WHERE g.guarantor.id = :guarantorId AND g.guaranteeType = :type
            ORDER BY g.id DESC
            """)
    List<LoanGuaranteeEntity> findCoSignerByGuarantorId(
            @Param("guarantorId") Long guarantorId,
            @Param("type") GuaranteeType type
    );
}
