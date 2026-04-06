package com.peerlending.persistence.repository;

import com.peerlending.persistence.entity.PaymentInstallmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentInstallmentRepository extends JpaRepository<PaymentInstallmentEntity, Long> {

    List<PaymentInstallmentEntity> findByLoanIdOrderByInstallmentNumber(Long loanId);

    Optional<PaymentInstallmentEntity> findByIdAndLoanId(Long id, Long loanId);
}
