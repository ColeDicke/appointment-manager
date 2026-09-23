package com.appointmentmanager.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public static final String CONFIRM_EMAIL = "CONFIRM_EMAIL";

    private final SupabaseAuthService authService;

    public AuthController(SupabaseAuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> login(@RequestParam String email, @RequestParam String password) {
        try {
            return ResponseEntity.ok(encodeSession(authService.login(normalizeEmail(email), password)));
        } catch (RestClientException | IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        }
    }

    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> signUp(@RequestParam String email, @RequestParam String password) {
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body("Password must contain at least 8 characters.");
        }
        try {
            SupabaseAuthService.AuthResult result = authService.signUp(normalizeEmail(email), password);
            if (result.confirmationRequired()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(CONFIRM_EMAIL);
            }
            return ResponseEntity.ok(encodeSession(result));
        } catch (RestClientException | IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body("Could not create that account.");
        }
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> refresh(@RequestParam String refreshToken) {
        try {
            return ResponseEntity.ok(encodeSession(authService.refresh(refreshToken)));
        } catch (RestClientException | IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired.");
        }
    }

    private String encodeSession(SupabaseAuthService.AuthResult result) {
        if (result.accessToken() == null || result.refreshToken() == null) {
            throw new IllegalArgumentException("Supabase did not return a complete session.");
        }
        return result.accessToken() + "\n" + result.refreshToken();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
