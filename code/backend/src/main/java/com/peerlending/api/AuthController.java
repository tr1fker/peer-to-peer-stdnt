package com.peerlending.api;

import com.peerlending.api.dto.GithubRegisterCompleteRequest;
import com.peerlending.api.dto.LoginRequest;
import com.peerlending.api.dto.RefreshRequest;
import com.peerlending.api.dto.RegisterRequest;
import com.peerlending.api.dto.RegistrationSubmittedResponse;
import com.peerlending.api.dto.ResendVerificationRequest;
import com.peerlending.api.dto.TokenResponse;
import com.peerlending.api.dto.VerifyEmailRequest;
import com.peerlending.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String GITHUB_PLACEHOLDER = "github-not-configured";
    private static final String GOOGLE_PLACEHOLDER = "google-not-configured";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/oauth/github")
    public Map<String, Boolean> githubOauthConfigured(
            @Value("${spring.security.oauth2.client.registration.github.client-id}") String githubClientId
    ) {
        boolean ok = githubClientId != null
                && !githubClientId.isBlank()
                && !GITHUB_PLACEHOLDER.equals(githubClientId.trim());
        return Map.of("configured", ok);
    }

    @GetMapping("/oauth/google")
    public Map<String, Boolean> googleOauthConfigured(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId
    ) {
        boolean ok = googleClientId != null
                && !googleClientId.isBlank()
                && !GOOGLE_PLACEHOLDER.equals(googleClientId.trim());
        return Map.of("configured", ok);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationSubmittedResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    public TokenResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
    }

    @PostMapping("/register/github-complete")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse completeGithubRegister(@Valid @RequestBody GithubRegisterCompleteRequest request) {
        return authService.completeOauthRegistration(request);
    }

    @PostMapping("/register/oauth-complete")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse completeOauthRegister(@Valid @RequestBody GithubRegisterCompleteRequest request) {
        return authService.completeOauthRegistration(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }
}
