package com.peerlending.application;

import com.peerlending.api.dto.AddCoSignerRequest;
import com.peerlending.api.dto.CoSignerInvitationDto;
import com.peerlending.api.dto.GuaranteeDto;
import com.peerlending.common.exception.ConflictException;
import com.peerlending.common.exception.ForbiddenOperationException;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.domain.GuaranteeStatus;
import com.peerlending.domain.GuaranteeType;
import com.peerlending.persistence.entity.LoanEntity;
import com.peerlending.persistence.entity.LoanGuaranteeEntity;
import com.peerlending.persistence.entity.StudentProfileEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.LoanGuaranteeRepository;
import com.peerlending.persistence.repository.LoanRepository;
import com.peerlending.persistence.repository.StudentProfileRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GuaranteeService {

    private final LoanRepository loanRepository;
    private final LoanGuaranteeRepository loanGuaranteeRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public GuaranteeService(
            LoanRepository loanRepository,
            LoanGuaranteeRepository loanGuaranteeRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository
    ) {
        this.loanRepository = loanRepository;
        this.loanGuaranteeRepository = loanGuaranteeRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional
    public GuaranteeDto addCoSigner(Long loanId, Long borrowerId, AddCoSignerRequest request) {
        LoanEntity loan = loanRepository.findById(loanId).orElseThrow(() -> new NotFoundException("Loan not found"));
        if (!loan.getLoanRequest().getBorrower().getId().equals(borrowerId)) {
            throw new ForbiddenOperationException("Only borrower can add co-signer guarantee");
        }
        UserEntity borrowerAccount = userRepository.findById(borrowerId).orElseThrow(() -> new NotFoundException("User not found"));
        if (borrowerAccount.isBlocked()) {
            throw new ForbiddenOperationException("Аккаунт заблокирован администратором.");
        }
        if (request.guarantorUserId().equals(borrowerId)) {
            throw new ForbiddenOperationException("Guarantor must be another user");
        }
        UserEntity guarantor = userRepository.findById(request.guarantorUserId())
                .orElseThrow(() -> new NotFoundException("Guarantor not found"));
        if (guarantor.isBlocked()) {
            throw new ForbiddenOperationException("Аккаунт выбранного поручителя заблокирован.");
        }

        for (LoanGuaranteeEntity g : loanGuaranteeRepository.findByLoanId(loanId)) {
            if (g.getGuaranteeType() != GuaranteeType.CO_SIGNER || g.getGuarantor() == null) {
                continue;
            }
            if (!g.getGuarantor().getId().equals(request.guarantorUserId())) {
                continue;
            }
            if (g.getStatus() == GuaranteeStatus.PENDING || g.getStatus() == GuaranteeStatus.ACTIVE) {
                throw new ConflictException("Этот пользователь уже указан поручителем по займу (ожидает ответа или уже согласился).");
            }
        }

        BigDecimal pendingOrActiveCoSignerSum = loanGuaranteeRepository.findByLoanId(loanId).stream()
                .filter(g -> g.getGuaranteeType() == GuaranteeType.CO_SIGNER)
                .filter(g -> g.getStatus() == GuaranteeStatus.PENDING || g.getStatus() == GuaranteeStatus.ACTIVE)
                .map(LoanGuaranteeEntity::getCoverageAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (pendingOrActiveCoSignerSum.add(request.coverageAmount()).compareTo(loan.getPrincipal()) > 0) {
            throw new ForbiddenOperationException(
                    "Сумма поручительств (уже приглашённые и активные) не может превышать тело займа."
            );
        }

        LoanGuaranteeEntity g = new LoanGuaranteeEntity();
        g.setLoan(loan);
        g.setGuarantor(guarantor);
        g.setGuaranteeType(GuaranteeType.CO_SIGNER);
        g.setCoverageAmount(request.coverageAmount());
        g.setStatus(GuaranteeStatus.PENDING);
        g = loanGuaranteeRepository.save(g);

        return toGuaranteeDto(g);
    }

    @Transactional(readOnly = true)
    public List<CoSignerInvitationDto> listInvitationsForGuarantor(Long guarantorUserId) {
        return loanGuaranteeRepository
                .findCoSignerByGuarantorId(guarantorUserId, GuaranteeType.CO_SIGNER)
                .stream()
                .map(this::toInvitationDto)
                .toList();
    }

    @Transactional
    public GuaranteeDto acceptInvitation(Long guaranteeId, Long guarantorUserId) {
        LoanGuaranteeEntity g = loanGuaranteeRepository.findById(guaranteeId)
                .orElseThrow(() -> new NotFoundException("Запись поручительства не найдена"));
        if (g.getGuaranteeType() != GuaranteeType.CO_SIGNER || g.getGuarantor() == null
                || !g.getGuarantor().getId().equals(guarantorUserId)) {
            throw new ForbiddenOperationException("Нет доступа к этому приглашению.");
        }
        if (g.getStatus() != GuaranteeStatus.PENDING) {
            throw new ConflictException("Приглашение уже обработано.");
        }
        UserEntity guarantor = userRepository.findById(guarantorUserId).orElseThrow();
        if (guarantor.isBlocked()) {
            throw new ForbiddenOperationException("Аккаунт заблокирован — нельзя принять поручительство.");
        }
        g.setStatus(GuaranteeStatus.ACTIVE);
        g = loanGuaranteeRepository.save(g);
        return toGuaranteeDto(g);
    }

    @Transactional
    public GuaranteeDto declineInvitation(Long guaranteeId, Long guarantorUserId) {
        LoanGuaranteeEntity g = loanGuaranteeRepository.findById(guaranteeId)
                .orElseThrow(() -> new NotFoundException("Запись поручительства не найдена"));
        if (g.getGuaranteeType() != GuaranteeType.CO_SIGNER || g.getGuarantor() == null
                || !g.getGuarantor().getId().equals(guarantorUserId)) {
            throw new ForbiddenOperationException("Нет доступа к этому приглашению.");
        }
        if (g.getStatus() != GuaranteeStatus.PENDING) {
            throw new ConflictException("Приглашение уже обработано.");
        }
        g.setStatus(GuaranteeStatus.DECLINED);
        g = loanGuaranteeRepository.save(g);
        return toGuaranteeDto(g);
    }

    private CoSignerInvitationDto toInvitationDto(LoanGuaranteeEntity g) {
        LoanEntity loan = g.getLoan();
        UserEntity borrower = loan.getLoanRequest().getBorrower();
        String fullName = studentProfileRepository.findById(borrower.getId())
                .map(StudentProfileEntity::getFullName)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse("—");
        return new CoSignerInvitationDto(
                g.getId(),
                loan.getId(),
                loan.getLoanRequest().getId(),
                borrower.getEmail(),
                fullName,
                loan.getPrincipal(),
                g.getCoverageAmount(),
                g.getStatus()
        );
    }

    private static GuaranteeDto toGuaranteeDto(LoanGuaranteeEntity g) {
        return new GuaranteeDto(
                g.getId(),
                g.getLoan().getId(),
                g.getGuarantor() != null ? g.getGuarantor().getId() : null,
                g.getGuaranteeType(),
                g.getCoverageAmount(),
                g.getStatus()
        );
    }
}
