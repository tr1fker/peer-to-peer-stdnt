package com.peerlending.api;

import com.peerlending.application.GithubPublicApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/external/github")
public class GithubPublicController {

    private final GithubPublicApiClient githubPublicApiClient;

    public GithubPublicController(GithubPublicApiClient githubPublicApiClient) {
        this.githubPublicApiClient = githubPublicApiClient;
    }

    @GetMapping("/users/{login}")
    public ResponseEntity<Map<String, Object>> user(@PathVariable String login) {
        return githubPublicApiClient.fetchUser(login)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
