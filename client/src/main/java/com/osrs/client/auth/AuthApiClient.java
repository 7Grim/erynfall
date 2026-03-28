package com.osrs.client.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

public class AuthApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(AuthApiClient.class);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public AuthApiClient() {
        this.baseUrl = resolveBaseUrl();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.mapper = new ObjectMapper();
    }

    public LoginResult login(String email, String password) {
        try {
            LOG.info("Auth login request to {} for {}", baseUrl, email);
            String body = mapper.writeValueAsString(Map.of(
                "email", email,
                "password", password
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = safeReadJson(response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new LoginResult(
                    true,
                    "",
                    json.path("accessToken").asText(""),
                    json.path("refreshToken").asText(""),
                    json.path("characterName").asText(""),
                    json.path("accountId").asInt(0),
                    json.path("characterId").asInt(0)
                );
            }

            String error = json.path("error").asText("");
            if (error.isBlank()) {
                error = "Authentication failed (" + response.statusCode() + ").";
            }
            return new LoginResult(false, error, "", "", "", 0, 0);
        } catch (Exception e) {
            LOG.warn("Auth login request failed for {}: {}", email, e.toString());
            return new LoginResult(false,
                "Unable to reach auth service: " + e.getClass().getSimpleName() +
                    (e.getMessage() == null || e.getMessage().isBlank() ? "" : " (" + e.getMessage() + ")"),
                "", "", "", 0, 0);
        }
    }

    private JsonNode safeReadJson(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return mapper.createObjectNode();
        }
        return mapper.readTree(json);
    }

    private static String resolveBaseUrl() {
        // 1. System property (highest priority - for dev overrides)
        String configured = System.getProperty("AUTH_BASE_URL");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AUTH_BASE_URL");
        }

        // 2. Bundled properties file
        if (configured == null || configured.isBlank()) {
            try (InputStream is = AuthApiClient.class.getResourceAsStream("/auth.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    configured = props.getProperty("auth.base.url");
                }
            } catch (Exception e) {
                LOG.warn("Could not read auth.properties: {}", e.getMessage());
            }
        }

        // 3. Hardcoded fallback
        if (configured == null || configured.isBlank()) {
            return "http://localhost:8080";
        }

        String trimmed = configured.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public record LoginResult(
        boolean success,
        String errorMessage,
        String accessToken,
        String refreshToken,
        String characterName,
        int accountId,
        int characterId
    ) {
    }
}
