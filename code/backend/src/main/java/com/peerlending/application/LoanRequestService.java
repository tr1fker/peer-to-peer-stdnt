package com.peerlending.application;

import com.peerlending.common.annotation.LoggedUseCase;
import com.peerlending.api.dto.CreateLoanRequestPayload;
import com.peerlending.api.dto.LoanRequestDto;
import com.peerlending.api.dto.PageResponse;
import com.peerlending.common.exception.ForbiddenOperationException;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.persistence.entity.InvestmentEntity;
import com.peerlending.persistence.entity.LoanRequestEntity;
import com.peerlending.persistence.entity.StudentProfileEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.InvestmentRepository;
import com.peerlending.persistence.repository.LoanRequestRepository;
import com.peerlending.persistence.repository.StudentProfileRepository;
import com.peerlending.persistence.repository.UserRepository;
import com.peerlending.persistence.spec.LoanRequestSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LoanRequestService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LoanRequestRepository loanRequestRepository;
    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CreditLimitService creditLimitService;
    private final EntityManager entityManager;

    public LoanRequestService(
            LoanRequestRepository loanRequestRepository,
            InvestmentRepository investmentRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            CreditLimitService creditLimitService,
            EntityManager entityManager
    ) {
        this.loanRequestRepository = loanRequestRepository;
        this.investmentRepository = investmentRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.creditLimitService = creditLimitService;
        this.entityManager = entityManager;
    }

    @LoggedUseCase("create-loan-request")
    @Transactional
    public LoanRequestDto create(Long borrowerId, CreateLoanRequestPayload payload) {
        UserEntity borrowerCheck = userRepository.findById(borrowerId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (borrowerCheck.isBlocked()) {
            throw new ForbiddenOperationException("Аккаунт заблокирован администратором. Новые заявки недоступны.");
        }
        if (!creditLimitService.hasVerifiedAcademicRecord(borrowerId)) {
            throw new ForbiddenOperationException(
                    "Сначала нужна подтверждённая администратором запись об успеваемости"
            );
        }
        BigDecimal max = creditLimitService.maxLoanAmountForBorrower(borrowerId);
        if (payload.amount().compareTo(max) > 0) {
            throw new ForbiddenOperationException("Amount exceeds credit limit based on verified academic record: " + max);
        }
        LoanRequestEntity e = new LoanRequestEntity();
        e.setBorrower(borrowerCheck);
        e.setAmount(payload.amount());
        e.setTermMonths(payload.termMonths());
        e.setPurpose(payload.purpose());
        e.setInterestRatePercent(payload.interestRatePercent());
        e.setStatus(LoanRequestStatus.OPEN);
        e = loanRequestRepository.save(e);
        return toDto(e);
    }

    @Transactional(readOnly = true)
    public List<LoanRequestDto> listForBorrower(Long borrowerId) {
        return loanRequestRepository.findByBorrower_IdOrderByCreatedAtDesc(borrowerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanRequestDto> search(
            Optional<LoanRequestStatus> status,
            Optional<BigDecimal> minAmount,
            Optional<BigDecimal> maxAmount,
            Optional<Long> borrowerId,
            int page,
            int size,
            String sortField,
            String sortDir
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = canonicalSortField(sortField);

        Specification<LoanRequestEntity> spec = LoanRequestSpecifications.withFilters(status, minAmount, maxAmount, borrowerId);

        Page<LoanRequestEntity> result;
        if ("reputation".equals(field)) {
            result = findPageSortedByReputation(spec, safePage, safeSize, direction);
        } else if ("funded".equals(field)) {
            result = findPageSortedByFundedAmount(spec, safePage, safeSize, direction);
        } else {
            result = loanRequestRepository.findAll(spec, PageRequest.of(safePage, safeSize, Sort.by(direction, field)));
        }

        Set<Long> borrowerIds = result.getContent().stream()
                .map(e -> e.getBorrower().getId())
                .collect(Collectors.toSet());
        Map<Long, Integer> reputationByBorrower = reputationByBorrowerIds(borrowerIds);
        List<LoanRequestDto> content = result.getContent().stream()
                .map(e -> toDto(
                        e,
                        reputationByBorrower.getOrDefault(e.getBorrower().getId(), ReputationService.DEFAULT_POINTS)
                ))
                .toList();
        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    private static String canonicalSortField(String raw) {
        if (raw == null || raw.isBlank()) {
            return "createdAt";
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "id" -> "id";
            case "amount" -> "amount";
            case "termmonths" -> "termMonths";
            case "interestratepercent" -> "interestRatePercent";
            case "status" -> "status";
            case "createdat" -> "createdAt";
            case "reputation" -> "reputation";
            case "funded" -> "funded";
            default -> "createdAt";
        };
    }

    private Page<LoanRequestEntity> findPageSortedByReputation(
            Specification<LoanRequestEntity> spec,
            int page,
            int size,
            Sort.Direction direction
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<LoanRequestEntity> countRoot = countQuery.from(LoanRequestEntity.class);
        Predicate countPred = spec.toPredicate(countRoot, countQuery, cb);
        if (countPred != null) {
            countQuery.where(countPred);
        }
        countQuery.select(cb.count(countRoot));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<LoanRequestEntity> q = cb.createQuery(LoanRequestEntity.class);
        Root<LoanRequestEntity> lr = q.from(LoanRequestEntity.class);
        Predicate pred = spec.toPredicate(lr, q, cb);
        if (pred != null) {
            q.where(pred);
        }

        Subquery<Integer> repSub = q.subquery(Integer.class);
        Root<StudentProfileEntity> sp = repSub.from(StudentProfileEntity.class);
        repSub.select(sp.get("reputationPoints"));
        repSub.where(cb.equal(sp.get("userId"), lr.get("borrower").get("id")));
        Expression<Integer> repExpr = cb.coalesce(repSub, cb.literal(ReputationService.DEFAULT_POINTS));

        List<Order> orders = new ArrayList<>(2);
        orders.add(direction.isAscending() ? cb.asc(repExpr) : cb.desc(repExpr));
        orders.add(cb.asc(lr.get("id")));
        q.orderBy(orders);
        q.select(lr);

        List<LoanRequestEntity> list = entityManager.createQuery(q)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        return new PageImpl<>(list, PageRequest.of(page, size), total);
    }

    private Page<LoanRequestEntity> findPageSortedByFundedAmount(
            Specification<LoanRequestEntity> spec,
            int page,
            int size,
            Sort.Direction direction
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<LoanRequestEntity> countRoot = countQuery.from(LoanRequestEntity.class);
        Predicate countPred = spec.toPredicate(countRoot, countQuery, cb);
        if (countPred != null) {
            countQuery.where(countPred);
        }
        countQuery.select(cb.count(countRoot));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<LoanRequestEntity> q = cb.createQuery(LoanRequestEntity.class);
        Root<LoanRequestEntity> lr = q.from(LoanRequestEntity.class);
        Predicate pred = spec.toPredicate(lr, q, cb);
        if (pred != null) {
            q.where(pred);
        }

        Subquery<BigDecimal> fundSub = q.subquery(BigDecimal.class);
        Root<InvestmentEntity> inv = fundSub.from(InvestmentEntity.class);
        fundSub.select(cb.coalesce(cb.sum(inv.get("amount")), cb.literal(BigDecimal.ZERO)));
        fundSub.where(cb.equal(inv.get("loanRequest").get("id"), lr.get("id")));

        List<Order> orders = new ArrayList<>(2);
        orders.add(direction.isAscending() ? cb.asc(fundSub) : cb.desc(fundSub));
        orders.add(cb.asc(lr.get("id")));
        q.orderBy(orders);
        q.select(lr);

        List<LoanRequestEntity> list = entityManager.createQuery(q)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        return new PageImpl<>(list, PageRequest.of(page, size), total);
    }

    @Transactional(readOnly = true)
    public List<LoanRequestDto> listDtosForLoanRequestsInOrder(List<Long> loanRequestIdsInOrder) {
        if (loanRequestIdsInOrder.isEmpty()) {
            return List.of();
        }
        Map<Long, LoanRequestEntity> map = loanRequestRepository.findAllById(loanRequestIdsInOrder).stream()
                .collect(Collectors.toMap(LoanRequestEntity::getId, Function.identity()));
        return loanRequestIdsInOrder.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanRequestDto get(Long id) {
        return loanRequestRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Loan request not found"));
    }

    @Transactional
    public void cancel(Long id, Long borrowerId) {
        LoanRequestEntity e = loanRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loan request not found"));
        if (!e.getBorrower().getId().equals(borrowerId)) {
            throw new ForbiddenOperationException("Not your loan request");
        }
        if (e.getStatus() != LoanRequestStatus.OPEN) {
            throw new ForbiddenOperationException("Only open requests can be cancelled");
        }
        BigDecimal funded = investmentRepository.sumAmountByLoanRequestId(id);
        if (funded.compareTo(BigDecimal.ZERO) > 0) {
            throw new ForbiddenOperationException("Cannot cancel a request that already has investments");
        }
        e.setStatus(LoanRequestStatus.CANCELLED);
        loanRequestRepository.save(e);
    }

    private LoanRequestDto toDto(LoanRequestEntity e) {
        return toDto(e, reputationPointsForBorrower(e.getBorrower().getId()));
    }

    private LoanRequestDto toDto(LoanRequestEntity e, int borrowerReputationPoints) {
        BigDecimal funded = investmentRepository.sumAmountByLoanRequestId(e.getId());
        return new LoanRequestDto(
                e.getId(),
                e.getBorrower().getId(),
                e.getAmount(),
                e.getTermMonths(),
                e.getPurpose(),
                e.getStatus(),
                e.getInterestRatePercent(),
                e.getCreatedAt(),
                funded,
                borrowerReputationPoints
        );
    }

    private int reputationPointsForBorrower(Long borrowerId) {
        return studentProfileRepository.findById(borrowerId)
                .map(StudentProfileEntity::getReputationPoints)
                .orElse(ReputationService.DEFAULT_POINTS);
    }

    private Map<Long, Integer> reputationByBorrowerIds(Set<Long> borrowerIds) {
        if (borrowerIds.isEmpty()) {
            return Map.of();
        }
        return studentProfileRepository.findAllById(borrowerIds).stream()
                .collect(Collectors.toMap(StudentProfileEntity::getUserId, StudentProfileEntity::getReputationPoints));
    }
}
