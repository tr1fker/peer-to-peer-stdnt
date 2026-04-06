package com.peerlending.persistence.spec;

import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.persistence.entity.LoanRequestEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LoanRequestSpecifications {

    private LoanRequestSpecifications() {
    }

    public static Specification<LoanRequestEntity> withFilters(
            Optional<LoanRequestStatus> status,
            Optional<BigDecimal> minAmount,
            Optional<BigDecimal> maxAmount,
            Optional<Long> borrowerId
    ) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            status.ifPresent(s -> preds.add(cb.equal(root.get("status"), s)));
            minAmount.ifPresent(m -> preds.add(cb.greaterThanOrEqualTo(root.get("amount"), m)));
            maxAmount.ifPresent(m -> preds.add(cb.lessThanOrEqualTo(root.get("amount"), m)));
            borrowerId.ifPresent(b -> preds.add(cb.equal(root.get("borrower").get("id"), b)));
            if (preds.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }
}
