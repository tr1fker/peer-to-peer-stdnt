package com.peerlending.security;

import com.peerlending.persistence.entity.RefreshTokenEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Runs in the caller's transaction (callers must use a read-write transaction). */
    @Transactional(readOnly = false)
    public String issue(UserEntity user, Instant expiresAt) {
        String raw = java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "");
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUser(user);
        entity.setTokenHash(sha256(raw));
        entity.setExpiresAt(expiresAt);
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
        return raw;
    }

    @Transactional(readOnly = false)
    public UserEntity validateAndRevoke(String rawToken) {
        String hash = sha256(rawToken);
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHashAndRevokedIsFalse(hash)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new com.peerlending.common.exception.ForbiddenOperationException("Invalid refresh token"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return token.getUser();
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
