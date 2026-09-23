package com.appointmentmanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class SupabaseAuthService {
    private final RestClient authClient;

    public SupabaseAuthService(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.publishable-key}") String publishableKey) {
        authClient = RestClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", publishableKey)
                .build();
    }

    public AuthResult login(String email, String password) {
        JsonNode response = authClient.post()
                .uri("/auth/v1/token?grant_type=password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(JsonNode.class);
        return requireSession(response);
    }

    public AuthResult refresh(String refreshToken) {
        JsonNode response = authClient.post()
                .uri("/auth/v1/token?grant_type=refresh_token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("refresh_token", refreshToken))
                .retrieve()
                .body(JsonNode.class);
        return requireSession(response);
    }

    public AuthResult signUp(String email, String password) {
        JsonNode response = authClient.post()
                .uri("/auth/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(JsonNode.class);

        String accessToken = textValue(response, "access_token");
        if (accessToken == null) {
            return new AuthResult(null, null, true);
        }
        return new AuthResult(accessToken, textValue(response, "refresh_token"), false);
    }

    /** Validates the JWT with Supabase Auth and returns its trusted user ID. */
    public UUID verifyAccessToken(String accessToken) {
        JsonNode response = authClient.get()
                .uri("/auth/v1/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);
        String userId = textValue(response, "id");
        if (userId == null) {
            throw new IllegalArgumentException("The access token has no user ID.");
        }
        return UUID.fromString(userId);
    }

    private AuthResult requireSession(JsonNode response) {
        String accessToken = textValue(response, "access_token");
        if (accessToken == null) {
            throw new IllegalArgumentException("Supabase did not return an access token.");
        }
        return new AuthResult(accessToken, textValue(response, "refresh_token"), false);
    }

    private String textValue(JsonNode response, String field) {
        if (response == null || response.path(field).isMissingNode() || response.path(field).isNull()) {
            return null;
        }
        String value = response.path(field).asText();
        return value.isBlank() ? null : value;
    }

    public record AuthResult(String accessToken, String refreshToken, boolean confirmationRequired) {
    }
}
