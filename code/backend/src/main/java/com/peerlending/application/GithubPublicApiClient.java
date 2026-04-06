package com.peerlending.application;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
public class GithubPublicApiClient {

    private final RestClient restClient;

    public GithubPublicApiClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.github.com").build();
    }

    public Optional<Map<String, Object>> fetchUser(String login) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri("/users/{login}", login)
                    .retrieve()
                    .body(Map.class);
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
