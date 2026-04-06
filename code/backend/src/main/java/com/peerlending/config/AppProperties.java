package com.peerlending.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Api api = new Api();
    private final Frontend frontend = new Frontend();
    private final Email email = new Email();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessMinutes = 15;
        private long refreshDays = 14;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public List<String> asList() {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Getter
    @Setter
    public static class Api {
        private String versionPrefix = "/api/v1";
    }

    /** Base URL of SPA (no trailing slash), for links in emails. */
    @Getter
    @Setter
    public static class Frontend {
        private String baseUrl = "http://localhost:5173";
    }

    @Getter
    @Setter
    public static class Email {
        /** Lifetime of email confirmation link. */
        private long verificationTtlHours = 48;
    }
}
