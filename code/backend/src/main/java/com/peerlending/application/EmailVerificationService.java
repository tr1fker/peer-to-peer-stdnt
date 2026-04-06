package com.peerlending.application;

import com.peerlending.common.exception.ForbiddenOperationException;
import com.peerlending.config.AppProperties;
import com.peerlending.persistence.entity.EmailVerificationTokenEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.EmailVerificationTokenRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailVerificationMailService mailService;
    private final AppProperties appProperties;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailVerificationMailService mailService,
            AppProperties appProperties
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.appProperties = appProperties;
    }

    @Transactional
    public void issueTokenAndSendEmail(UserEntity user) {
        tokenRepository.deleteByUser_Id(user.getId());
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
        String hash = sha256Hex(raw);
        EmailVerificationTokenEntity row = new EmailVerificationTokenEntity();
        row.setUser(user);
        row.setTokenHash(hash);
        row.setExpiresAt(Instant.now().plus(appProperties.getEmail().getVerificationTtlHours(), ChronoUnit.HOURS));
        tokenRepository.save(row);

        String base = appProperties.getFrontend().getBaseUrl().replaceAll("/$", "");
        String url = base + "/auth/verify-email?token=" + URLEncoder.encode(raw, StandardCharsets.UTF_8);
        mailService.sendVerificationLink(user, url);
    }

    @Transactional
    public UserEntity verifyTokenAndActivateUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ForbiddenOperationException("Ссылка недействительна или устарела");
        }
        String hash = sha256Hex(rawToken.trim());
        EmailVerificationTokenEntity row = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ForbiddenOperationException("Ссылка недействительна или устарела"));
        if (row.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(row);
            throw new ForbiddenOperationException(
                    "Ссылка подтверждения истекла — на странице входа можно запросить письмо повторно"
            );
        }
        UserEntity user = row.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUser_Id(user.getId());
        return user;
    }

    private static byte[] randomBytes(int n) {
        byte[] buf = new byte[n];
        RANDOM.nextBytes(buf);
        return buf;
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
