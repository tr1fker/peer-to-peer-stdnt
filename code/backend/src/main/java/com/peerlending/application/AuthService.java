package com.peerlending.application;

import com.peerlending.api.dto.GithubRegisterCompleteRequest;
import com.peerlending.api.dto.LoginRequest;
import com.peerlending.api.dto.RefreshRequest;
import com.peerlending.api.dto.RegisterRequest;
import com.peerlending.api.dto.RegistrationSubmittedResponse;
import com.peerlending.api.dto.ResendVerificationRequest;
import com.peerlending.api.dto.TokenResponse;
import com.peerlending.api.dto.VerifyEmailRequest;
import com.peerlending.common.exception.ConflictException;
import com.peerlending.common.exception.ForbiddenOperationException;
import com.peerlending.common.exception.InvalidLoginException;
import com.peerlending.config.AppProperties;
import com.peerlending.persistence.entity.OAuthAccountEntity;
import com.peerlending.persistence.entity.RoleEntity;
import com.peerlending.persistence.entity.StudentProfileEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.OAuthAccountRepository;
import com.peerlending.persistence.repository.RoleRepository;
import com.peerlending.persistence.repository.StudentProfileRepository;
import com.peerlending.persistence.repository.UserRepository;
import com.peerlending.security.JwtTokenService;
import com.peerlending.security.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AppProperties appProperties;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            OAuthAccountRepository oauthAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            AppProperties appProperties,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.appProperties = appProperties;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional(readOnly = false)
    public RegistrationSubmittedResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    "Этот email уже зарегистрирован. Войдите или запросите письмо подтверждения повторно со страницы входа."
            );
        }
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setEmailVerified(false);
        Set<RoleEntity> roles = new HashSet<>();
        roleRepository.findByName("ROLE_BORROWER").ifPresent(roles::add);
        roleRepository.findByName("ROLE_LENDER").ifPresent(roles::add);
        user.setRoles(roles);
        user = userRepository.save(user);

        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setUser(user);
        profile.setFullName(request.fullName());
        profile.setUniversity(request.university());
        profile.setStudentGroup(request.studentGroup());
        studentProfileRepository.save(profile);

        emailVerificationService.issueTokenAndSendEmail(user);
        return new RegistrationSubmittedResponse(
                email,
                "На указанный адрес отправлена ссылка для подтверждения. Перейдите по ней, затем войдите с паролем."
        );
    }

    @Transactional(readOnly = false)
    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidLoginException("Неверный email или пароль"));
        if (user.getPasswordHash() == null) {
            throw new InvalidLoginException(
                    "Этот аккаунт создан через соцвход — войдите через GitHub или Google или зарегистрируйте другой email"
            );
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidLoginException("Неверный email или пароль");
        }
        if (!user.isEnabled()) {
            throw new InvalidLoginException("Аккаунт отключён");
        }
        if (!user.isEmailVerified()) {
            throw new InvalidLoginException(
                    "Сначала подтвердите email по ссылке из письма. Не пришло — запросите повторно на странице входа."
            );
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = false)
    public TokenResponse verifyEmail(VerifyEmailRequest request) {
        UserEntity user = emailVerificationService.verifyTokenAndActivateUser(request.token());
        return issueTokens(user);
    }

    @Transactional(readOnly = false)
    public void resendVerification(ResendVerificationRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.getPasswordHash() == null || user.isEmailVerified()) {
            return;
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidLoginException("Неверный email или пароль");
        }
        emailVerificationService.issueTokenAndSendEmail(user);
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    @Transactional(readOnly = false)
    public TokenResponse refresh(RefreshRequest request) {
        UserEntity user = refreshTokenService.validateAndRevoke(request.refreshToken());
        if (!user.isEmailVerified() && user.getPasswordHash() != null) {
            throw new com.peerlending.common.exception.ForbiddenOperationException(
                    "Подтвердите email перед продолжением"
            );
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = false)
    public TokenResponse completeOauthRegistration(GithubRegisterCompleteRequest request) {
        JwtTokenService.OauthPendingRegistrationClaims pending = jwtTokenService
                .parseOauthPendingRegistrationToken(request.pendingToken())
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Срок ссылки истёк — начните вход через GitHub или Google снова"));
        if (oauthAccountRepository.findByProviderAndProviderUserId(pending.provider(), pending.providerUserId())
                .isPresent()) {
            throw new ConflictException(oauthAlreadyRegisteredMessage(pending.provider()));
        }
        String email = resolveEmailForOauthSignup(pending);
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setEnabled(true);
        user.setEmailVerified(true);
        Set<RoleEntity> roles = new HashSet<>();
        roleRepository.findByName("ROLE_BORROWER").ifPresent(roles::add);
        roleRepository.findByName("ROLE_LENDER").ifPresent(roles::add);
        user.setRoles(roles);
        user = userRepository.save(user);

        OAuthAccountEntity link = new OAuthAccountEntity();
        link.setUser(user);
        link.setProvider(pending.provider());
        link.setProviderUserId(pending.providerUserId());
        oauthAccountRepository.save(link);

        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setUser(user);
        profile.setFullName(request.fullName());
        profile.setUniversity(request.university());
        profile.setStudentGroup(request.studentGroup());
        studentProfileRepository.save(profile);

        return issueTokens(user);
    }

    private static String oauthAlreadyRegisteredMessage(String provider) {
        if ("google".equals(provider)) {
            return "Этот Google-аккаунт уже зарегистрирован — войдите через Google";
        }
        return "Этот GitHub уже зарегистрирован — войдите через GitHub";
    }

    private String resolveEmailForOauthSignup(JwtTokenService.OauthPendingRegistrationClaims pending) {
        String fromOauth = pending.email();
        if (fromOauth != null && !fromOauth.isBlank()) {
            String e = fromOauth.trim().toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(e)) {
                throw new ConflictException(
                        "Такой email уже занят — войдите с паролем и привяжите "
                                + providerLabel(pending.provider())
                                + " в профиле"
                );
            }
            return e;
        }
        String login = pending.login() == null || pending.login().isBlank() ? "user" : pending.login();
        String suffix = "google".equals(pending.provider()) ? "google" : "github";
        return login + "+" + pending.providerUserId() + "@" + suffix + ".oauth.peerlending.local";
    }

    private static String providerLabel(String provider) {
        return "google".equals(provider) ? "Google" : "GitHub";
    }

    private TokenResponse issueTokens(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet());
        String access = jwtTokenService.createAccessToken(user.getId(), user.getEmail(), roleNames);
        Instant refreshExp = Instant.now().plusSeconds(appProperties.getJwt().getRefreshDays() * 24 * 60 * 60);
        String refresh = refreshTokenService.issue(user, refreshExp);
        return new TokenResponse(access, refresh, appProperties.getJwt().getAccessMinutes() * 60);
    }
}
