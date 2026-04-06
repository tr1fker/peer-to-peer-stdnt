package com.peerlending.persistence.repository;

import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.persistence.entity.LoanRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LoanRequestRepository extends JpaRepository<LoanRequestEntity, Long>, JpaSpecificationExecutor<LoanRequestEntity> {

    long countByBorrower_IdAndStatus(Long borrowerId, LoanRequestStatus status);

    long countByStatus(LoanRequestStatus status);

    List<LoanRequestEntity> findByBorrower_IdOrderByCreatedAtDesc(Long borrowerId);

    Page<LoanRequestEntity> findByBorrowerId(Long borrowerId, Pageable pageable);

    Page<LoanRequestEntity> findByStatus(LoanRequestStatus status, Pageable pageable);

    @Query(
            value = """
                    select cast(lr.created_at at time zone 'UTC' as date), count(*)
                    from loan_requests lr
                    where lr.created_at >= cast(:from as timestamptz)
                    group by 1
                    order by 1
                    """,
            nativeQuery = true
    )
    List<Object[]> countNewLoanRequestsByDayUtc(@Param("from") Instant from);
}
