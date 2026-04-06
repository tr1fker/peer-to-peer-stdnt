package com.peerlending.persistence.repository;

import com.peerlending.persistence.entity.InvestmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface InvestmentRepository extends JpaRepository<InvestmentEntity, Long> {

    List<InvestmentEntity> findByLoanRequestId(Long loanRequestId);

    List<InvestmentEntity> findByLenderId(Long lenderId);

    boolean existsByLender_IdAndLoanRequest_Id(Long lenderId, Long loanRequestId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM InvestmentEntity i WHERE i.loanRequest.id = :requestId")
    BigDecimal sumAmountByLoanRequestId(@Param("requestId") Long requestId);
}
