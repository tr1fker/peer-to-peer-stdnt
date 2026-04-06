package com.peerlending.api;

import com.peerlending.api.dto.CoSignerInvitationDto;
import com.peerlending.api.dto.GuaranteeDto;
import com.peerlending.api.dto.LoanRequestDto;
import com.peerlending.api.dto.MeAccountDto;
import com.peerlending.api.dto.MyInvestmentDto;
import com.peerlending.api.dto.StudentProfileDto;
import com.peerlending.api.dto.UpdateProfileRequest;
import com.peerlending.application.CreditLimitService;
import com.peerlending.application.GuaranteeService;
import com.peerlending.application.LoanQueryService;
import com.peerlending.application.LoanRequestFavoriteService;
import com.peerlending.application.LoanRequestService;
import com.peerlending.application.PortfolioExportService;
import com.peerlending.application.StudentProfileService;
import com.peerlending.common.exception.ConflictException;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.OAuthAccountRepository;
import com.peerlending.persistence.repository.UserRepository;
import com.peerlending.security.AuthPrincipal;
import com.peerlending.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final StudentProfileService studentProfileService;
    private final CreditLimitService creditLimitService;
    private final OAuthAccountRepository oauthAccountRepository;
    private final LoanRequestService loanRequestService;
    private final LoanQueryService loanQueryService;
    private final UserRepository userRepository;
    private final GuaranteeService guaranteeService;
    private final LoanRequestFavoriteService loanRequestFavoriteService;
    private final PortfolioExportService portfolioExportService;

    public MeController(
            StudentProfileService studentProfileService,
            CreditLimitService creditLimitService,
            OAuthAccountRepository oauthAccountRepository,
            LoanRequestService loanRequestService,
            LoanQueryService loanQueryService,
            UserRepository userRepository,
            GuaranteeService guaranteeService,
            LoanRequestFavoriteService loanRequestFavoriteService,
            PortfolioExportService portfolioExportService
    ) {
        this.studentProfileService = studentProfileService;
        this.creditLimitService = creditLimitService;
        this.oauthAccountRepository = oauthAccountRepository;
        this.loanRequestService = loanRequestService;
        this.loanQueryService = loanQueryService;
        this.userRepository = userRepository;
        this.guaranteeService = guaranteeService;
        this.loanRequestFavoriteService = loanRequestFavoriteService;
        this.portfolioExportService = portfolioExportService;
    }

    @GetMapping("/account")
    public MeAccountDto account(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        long uid = principal.id();
        boolean githubLinked = oauthAccountRepository.existsByUser_IdAndProvider(uid, "github");
        boolean googleLinked = oauthAccountRepository.existsByUser_IdAndProvider(uid, "google");
        boolean blocked = userRepository.findById(uid).map(UserEntity::isBlocked).orElse(false);
        return new MeAccountDto(principal.id(), principal.email(), roles, githubLinked, googleLinked, blocked);
    }

    @PostMapping("/oauth/github/link-start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startGithubLink(HttpServletRequest request, @AuthenticationPrincipal AuthPrincipal principal) {
        startOauthLink(request, principal, "github", "GitHub");
    }

    @PostMapping("/oauth/google/link-start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startGoogleLink(HttpServletRequest request, @AuthenticationPrincipal AuthPrincipal principal) {
        startOauthLink(request, principal, "google", "Google");
    }

    private void startOauthLink(
            HttpServletRequest request,
            AuthPrincipal principal,
            String provider,
            String label
    ) {
        if (oauthAccountRepository.existsByUser_IdAndProvider(principal.id(), provider)) {
            throw new ConflictException(label + " уже привязан к этому аккаунту");
        }
        var session = request.getSession(true);
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_OAUTH_LINK_USER_ID, principal.id());
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_OAUTH_LINK_PROVIDER, provider);
    }

    @GetMapping("/profile")
    public StudentProfileDto profile(@AuthenticationPrincipal AuthPrincipal principal) {
        return studentProfileService.getForUser(principal.id());
    }

    @PutMapping("/profile")
    public StudentProfileDto updateProfile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return studentProfileService.update(principal.id(), request);
    }

    @GetMapping("/loan-requests")
    public List<LoanRequestDto> myLoanRequests(@AuthenticationPrincipal AuthPrincipal principal) {
        return loanRequestService.listForBorrower(principal.id());
    }

    @GetMapping("/investments")
    public List<MyInvestmentDto> myInvestments(@AuthenticationPrincipal AuthPrincipal principal) {
        return loanQueryService.myInvestments(principal.id());
    }

    @GetMapping("/guarantor-invitations")
    public List<CoSignerInvitationDto> guarantorInvitations(@AuthenticationPrincipal AuthPrincipal principal) {
        return guaranteeService.listInvitationsForGuarantor(principal.id());
    }

    @PostMapping("/guarantor-invitations/{guaranteeId}/accept")
    public GuaranteeDto acceptGuarantorInvitation(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long guaranteeId
    ) {
        return guaranteeService.acceptInvitation(guaranteeId, principal.id());
    }

    @PostMapping("/guarantor-invitations/{guaranteeId}/decline")
    public GuaranteeDto declineGuarantorInvitation(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long guaranteeId
    ) {
        return guaranteeService.declineInvitation(guaranteeId, principal.id());
    }

    @GetMapping("/credit-limit")
    public Map<String, Object> creditLimit(@AuthenticationPrincipal AuthPrincipal principal) {
        Long userId = principal.id();
        BigDecimal max = creditLimitService.maxLoanAmountForBorrower(userId);
        boolean canCreate = creditLimitService.hasVerifiedAcademicRecord(userId);
        boolean accountBlocked = userRepository.findById(userId).map(UserEntity::isBlocked).orElse(false);
        return Map.of(
                "maxAmount", max,
                "canCreateLoanRequest", canCreate,
                "accountBlocked", accountBlocked
        );
    }

    @GetMapping("/favorite-loan-requests")
    public List<LoanRequestDto> favoriteLoanRequests(@AuthenticationPrincipal AuthPrincipal principal) {
        return loanRequestFavoriteService.listFavorites(principal.id());
    }

    @GetMapping("/favorite-loan-request-ids")
    public List<Long> favoriteLoanRequestIds(@AuthenticationPrincipal AuthPrincipal principal) {
        return loanRequestFavoriteService.listFavoriteLoanRequestIds(principal.id());
    }

    @PostMapping("/favorite-loan-requests/{loanRequestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFavoriteLoanRequest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long loanRequestId
    ) {
        loanRequestFavoriteService.add(principal.id(), loanRequestId);
    }

    @DeleteMapping("/favorite-loan-requests/{loanRequestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavoriteLoanRequest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long loanRequestId
    ) {
        loanRequestFavoriteService.remove(principal.id(), loanRequestId);
    }

    @GetMapping(value = "/portfolio-export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportPortfolioCsv(@AuthenticationPrincipal AuthPrincipal principal) {
        String csv = portfolioExportService.buildCsv(principal.id());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"peerlend-portfolio.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}
