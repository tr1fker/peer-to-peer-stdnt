package com.peerlending.security;

import com.peerlending.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    private static final String OAUTH_PENDING_SUBJECT = "oauth-register-pending";
    private static final String LEGACY_GITHUB_PENDING_SUBJECT = "github-register-pending";

    private final AppProperties appProperties;

    public JwtTokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String createAccessToken(Long userId, String email, Set<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(appProperties.getJwt().getAccessMinutes() * 60);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles.stream().sorted().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    public Optional<Claims> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Set<String> extractRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toSet());
        }
        return Set.of();
    }

    public String createOauthPendingRegistrationToken(
            String provider,
            String providerUserId,
            String login,
            String emailOrNull,
            String suggestedFullNameOrNull
    ) {
        Instant now = Instant.now();
        String loginSafe = login != null ? login : "";
        String nameHint = suggestedFullNameOrNull != null && !suggestedFullNameOrNull.isBlank()
                ? suggestedFullNameOrNull
                : (!loginSafe.isBlank() ? loginSafe : "user");
        return Jwts.builder()
                .subject(OAUTH_PENDING_SUBJECT)
                .claim("provider", provider)
                .claim("providerUserId", providerUserId)
                .claim("login", loginSafe)
                .claim("email", emailOrNull != null && !emailOrNull.isBlank() ? emailOrNull : "")
                .claim("nameHint", nameHint)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(30 * 60)))
                .signWith(signingKey())
                .compact();
    }

    /** Parses pending-registration JWTs (unified oauth-register-pending and legacy github-register-pending). */
    public Optional<OauthPendingRegistrationClaims> parseOauthPendingRegistrationToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subj = claims.getSubject();
            if (OAUTH_PENDING_SUBJECT.equals(subj)) {
                Object pid = claims.get("providerUserId");
                Object prov = claims.get("provider");
                if (pid == null || prov == null) {
                    return Optional.empty();
                }
                Object login = claims.get("login");
                return Optional.of(new OauthPendingRegistrationClaims(
                        String.valueOf(prov),
                        String.valueOf(pid),
                        login != null ? String.valueOf(login) : "",
                        claims.get("email", String.class) != null ? claims.get("email", String.class) : "",
                        claims.get("nameHint", String.class) != null ? claims.get("nameHint", String.class) : ""
                ));
            }
            if (LEGACY_GITHUB_PENDING_SUBJECT.equals(subj)) {
                Object gid = claims.get("githubId");
                if (gid == null) {
                    return Optional.empty();
                }
                Object login = claims.get("login");
                return Optional.of(new OauthPendingRegistrationClaims(
                        "github",
                        String.valueOf(gid),
                        login != null ? String.valueOf(login) : "",
                        claims.get("email", String.class) != null ? claims.get("email", String.class) : "",
                        claims.get("nameHint", String.class) != null ? claims.get("nameHint", String.class) : ""
                ));
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public record OauthPendingRegistrationClaims(
            String provider,
            String providerUserId,
            String login,
            String email,
            String nameHint
    ) {
    }

    private SecretKey signingKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
