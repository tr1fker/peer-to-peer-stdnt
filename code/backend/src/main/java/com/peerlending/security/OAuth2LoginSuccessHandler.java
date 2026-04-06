package com.peerlending.security;

import com.peerlending.persistence.entity.OAuthAccountEntity;
import com.peerlending.persistence.entity.RoleEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.OAuthAccountRepository;
import com.peerlending.persistence.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_OAUTH_LINK_USER_ID = "OAUTH_LINK_USER_ID";
    public static final String SESSION_OAUTH_LINK_PROVIDER = "OAUTH_LINK_PROVIDER";

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final long refreshDaysSeconds;

    public OAuth2LoginSuccessHandler(
            OAuthAccountRepository oauthAccountRepository,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            @Value("${app.jwt.refresh-days:14}") long refreshDays
    ) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.refreshDaysSeconds = refreshDays * 24 * 60 * 60;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return;
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        if (!"github".equals(registrationId) && !"google".equals(registrationId)) {
            response.sendError(HttpStatus.BAD_REQUEST.value());
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        Map<String, Object> attrs = oauthUser.getAttributes();

        String providerUserId;
        String login;
        String emailAttr = oauthUser.getAttribute("email");
        String nameAttr = oauthUser.getAttribute("name");

        if ("google".equals(registrationId)) {
            Object sub = attrs.get("sub");
            providerUserId = sub != null ? String.valueOf(sub) : "";
            if (providerUserId.isBlank()) {
                response.sendError(HttpStatus.BAD_REQUEST.value());
                return;
            }
            if (emailAttr != null && emailAttr.contains("@")) {
                login = emailAttr.substring(0, emailAttr.indexOf('@'));
            } else {
                login = "user";
            }
        } else {
            providerUserId = String.valueOf(attrs.get("id"));
            login = String.valueOf(attrs.getOrDefault("login", "user"));
        }

        var session = request.getSession(false);
        Long linkUserId = session != null ? (Long) session.getAttribute(SESSION_OAUTH_LINK_USER_ID) : null;
        String linkProvider = session != null ? (String) session.getAttribute(SESSION_OAUTH_LINK_PROVIDER) : null;
        if (linkUserId != null && linkProvider != null) {
            session.removeAttribute(SESSION_OAUTH_LINK_USER_ID);
            session.removeAttribute(SESSION_OAUTH_LINK_PROVIDER);
            if (!registrationId.equals(linkProvider)) {
                redirectProfileNotice(response, "oauth_link_wrong_provider");
                return;
            }
            handleOAuthLink(linkUserId, registrationId, providerUserId, response);
            return;
        }

        Optional<OAuthAccountEntity> existing = oauthAccountRepository.findByProviderAndProviderUserId(
                registrationId, providerUserId);
        if (existing.isPresent()) {
            issueTokensAndRedirect(existing.get().getUser(), response);
            return;
        }

        Optional<UserEntity> verifiedMatch = findVerifiedUserForTrustedOauthEmail(registrationId, attrs, emailAttr);
        if (verifiedMatch.isPresent()) {
            UserEntity user = verifiedMatch.get();
            if (oauthAccountRepository.existsByUser_IdAndProvider(user.getId(), registrationId)) {
                redirectProfileNotice(response, noticeCodeForAlreadyLinked(registrationId));
                return;
            }
            attachOAuthAndIssueTokens(user, registrationId, providerUserId, response);
            return;
        }

        String suggestedName = nameAttr != null && !nameAttr.isBlank() ? nameAttr : login;
        String pending = jwtTokenService.createOauthPendingRegistrationToken(
                registrationId,
                providerUserId,
                login,
                emailAttr,
                suggestedName
        );
        String url = frontendBaseUrl() + "/auth/oauth-register?pending=" + enc(pending);
        response.setStatus(HttpStatus.FOUND.value());
        response.sendRedirect(url);
    }

    private void handleOAuthLink(long userId, String provider, String providerUserId, HttpServletResponse response)
            throws IOException {
        if (oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).isPresent()) {
            redirectProfileNotice(response, noticeCodeForProviderInUse(provider));
            return;
        }
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            redirectProfileNotice(response, "oauth_link_session");
            return;
        }
        if (oauthAccountRepository.existsByUser_IdAndProvider(userId, provider)) {
            redirectProfileNotice(response, noticeCodeForAlreadyLinked(provider));
            return;
        }
        attachOAuthAndIssueTokens(user, provider, providerUserId, response);
    }

    /** Verified in-app email + trusted OAuth email → attach provider without oauth-register flow. */
    private Optional<UserEntity> findVerifiedUserForTrustedOauthEmail(
            String registrationId,
            Map<String, Object> attrs,
            String emailAttr
    ) {
        if (emailAttr == null || emailAttr.isBlank()) {
            return Optional.empty();
        }
        if ("google".equals(registrationId)) {
            if (!googleEmailVerified(attrs)) {
                return Optional.empty();
            }
        }
        String email = emailAttr.trim().toLowerCase();
        return userRepository.findByEmailIgnoreCase(email).filter(UserEntity::isEmailVerified);
    }

    private static boolean googleEmailVerified(Map<String, Object> attrs) {
        Object v = attrs.get("email_verified");
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof String s) {
            return "true".equalsIgnoreCase(s);
        }
        return false;
    }

    private void attachOAuthAndIssueTokens(
            UserEntity user,
            String provider,
            String providerUserId,
            HttpServletResponse response
    ) throws IOException {
        OAuthAccountEntity link = new OAuthAccountEntity();
        link.setUser(user);
        link.setProvider(provider);
        link.setProviderUserId(providerUserId);
        oauthAccountRepository.save(link);
        issueTokensAndRedirect(user, response);
    }

    private static String noticeCodeForProviderInUse(String provider) {
        return "google".equals(provider) ? "google_in_use" : "github_in_use";
    }

    private static String noticeCodeForAlreadyLinked(String provider) {
        return "google".equals(provider) ? "google_already_linked" : "github_already_linked";
    }

    private void issueTokensAndRedirect(UserEntity user, HttpServletResponse response) throws IOException {
        Set<String> roleNames = user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet());
        String access = jwtTokenService.createAccessToken(user.getId(), user.getEmail(), roleNames);
        String refresh = refreshTokenService.issue(user, Instant.now().plusSeconds(refreshDaysSeconds));
        String target = System.getenv().getOrDefault("FRONTEND_OAUTH_REDIRECT", "http://localhost:5173/auth/callback");
        String url = target + "?accessToken=" + enc(access) + "&refreshToken=" + enc(refresh);
        response.setStatus(HttpStatus.FOUND.value());
        response.sendRedirect(url);
    }

    private void redirectProfileNotice(HttpServletResponse response, String code) throws IOException {
        String url = frontendBaseUrl() + "/profile?notice=" + enc(code);
        response.setStatus(HttpStatus.FOUND.value());
        response.sendRedirect(url);
    }

    private static String frontendBaseUrl() {
        String callback = System.getenv().getOrDefault("FRONTEND_OAUTH_REDIRECT", "http://localhost:5173/auth/callback");
        String suffix = "/auth/callback";
        if (callback.endsWith(suffix)) {
            return callback.substring(0, callback.length() - suffix.length());
        }
        return "http://localhost:5173";
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
