package com.collectpro.backend.controller;
import com.collectpro.backend.dto.*;
import com.collectpro.backend.exception.InvalidCredentialsException;
import com.collectpro.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/activate")
    public ResponseEntity<UserResponse> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        UserResponse response = authService.activateAccount(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               @RequestHeader(value = "X-Client-Platform", required = false) String clientPlatform) {
        AuthResponse response = authService.login(request, clientPlatform);
        return buildAuthCookieResponse(response, clientPlatform);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshTokenRequest body,
                                                 @CookieValue(value = "refresh_token", required = false) String cookieToken,
                                                 @RequestHeader(value = "X-Client-Platform", required = false) String clientPlatform) {
        String rawToken = "WEB".equalsIgnoreCase(clientPlatform)
                ? cookieToken
                : (body != null ? body.getRefreshToken() : null);

        if (rawToken == null) {
            throw new InvalidCredentialsException("Refresh token manquant");
        }

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(rawToken);
        AuthResponse response = authService.refresh(request, clientPlatform);
        return buildAuthCookieResponse(response, clientPlatform);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String cookieToken) {
        if (cookieToken != null) {
            authService.revokeRefreshToken(cookieToken);
        }
        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie.toString()).build();
    }

    private ResponseEntity<AuthResponse> buildAuthCookieResponse(AuthResponse response, String clientPlatform) {
        if (!"WEB".equalsIgnoreCase(clientPlatform)) {
            return ResponseEntity.ok(response);
        }
        ResponseCookie cookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();

        // Pour le web, on ne renvoie plus le refreshToken dans le corps JSON
        response.setRefreshToken(null);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }

    @GetMapping("/activate")
    public ResponseEntity<ActivationTokenStatusResponse> checkActivationToken(@RequestParam String token) {
        ActivationTokenStatusResponse response = authService.checkActivationToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}