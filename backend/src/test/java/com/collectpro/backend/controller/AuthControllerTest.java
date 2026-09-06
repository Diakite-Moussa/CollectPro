package com.collectpro.backend.controller;

import com.collectpro.backend.dto.ActivateAccountRequest;
import com.collectpro.backend.dto.ActivationTokenStatusResponse;
import com.collectpro.backend.dto.AuthResponse;
import com.collectpro.backend.dto.ForgotPasswordRequest;
import com.collectpro.backend.dto.LoginRequest;
import com.collectpro.backend.dto.RefreshTokenRequest;
import com.collectpro.backend.dto.ResetPasswordRequest;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.exception.InvalidCredentialsException;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests du AuthController : login/refresh/logout/activation/reset password.
 * @PostMapping publics (permitAll dans SecurityConfig) -> pas besoin de
 * SecurityAuthorizationService ici, contrairement aux autres controllers.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    // Beans requis par SecurityConfig (JwtAuthenticationFilter) même si le
    // filtre lui-même est désactivé par addFilters = false.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private AuthResponse buildAuthResponse(String refreshToken) {
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("agent@test.com")
                .firstName("Agent")
                .lastName("Terrain")
                .role("AGENT")
                .build();

        return AuthResponse.builder()
                .accessToken("access-token-value")
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(user)
                .build();
    }

    // ---------------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /login - Sans header X-Client-Platform (ex: mobile), retourne le refreshToken dans le corps")
    void login_WithoutWebPlatform_ReturnsTokensInBody() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("agent@test.com");
        request.setPassword("Password123");

        when(authService.login(any(), isNull())).thenReturn(buildAuthResponse("raw-refresh-token"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.refreshToken").value("raw-refresh-token"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("POST /login - Avec X-Client-Platform=WEB, pose un cookie httpOnly et masque le refreshToken du corps")
    void login_WithWebPlatform_SetsCookieAndHidesRefreshToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("Password123");

        when(authService.login(any(), eq("WEB"))).thenReturn(buildAuthResponse("raw-refresh-token"));

        mockMvc.perform(post("/login")
                        .header("X-Client-Platform", "WEB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=raw-refresh-token")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")));
    }

    @Test
    @DisplayName("POST /login - Identifiants invalides -> 401")
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("agent@test.com");
        request.setPassword("WrongPassword");

        when(authService.login(any(), any()))
                .thenThrow(new InvalidCredentialsException("Email ou mot de passe incorrect"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    @DisplayName("POST /login - Email invalide (validation @Email) -> 400")
    void login_BlankEmail_ReturnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("pas-un-email");
        request.setPassword("Password123");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    // ---------------------------------------------------------------
    // REFRESH
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /refresh - Plateforme MOBILE, utilise le refreshToken du corps JSON")
    void refresh_Mobile_UsesBodyToken() throws Exception {
        RefreshTokenRequest body = new RefreshTokenRequest();
        body.setRefreshToken("mobile-refresh-token");

        when(authService.refresh(any(), eq("MOBILE"))).thenReturn(buildAuthResponse("new-refresh-token"));

        mockMvc.perform(post("/refresh")
                        .header("X-Client-Platform", "MOBILE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("POST /refresh - Plateforme WEB, utilise le refresh_token du cookie (ignore le corps)")
    void refresh_Web_UsesCookieToken() throws Exception {
        when(authService.refresh(any(), eq("WEB"))).thenReturn(buildAuthResponse("rotated-refresh-token"));

        mockMvc.perform(post("/refresh")
                        .header("X-Client-Platform", "WEB")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "cookie-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("rotated-refresh-token")));
    }

    @Test
    @DisplayName("POST /refresh - Aucun token fourni (ni cookie WEB, ni corps) -> 401")
    void refresh_NoTokenProvided_Returns401() throws Exception {
        mockMvc.perform(post("/refresh")
                        .header("X-Client-Platform", "WEB"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token manquant"));

        verify(authService, never()).refresh(any(), any());
    }

    // ---------------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /logout - Avec cookie, révoque le refresh token et efface le cookie")
    void logout_WithCookie_RevokesTokenAndClearsCookie() throws Exception {
        doNothing().when(authService).revokeRefreshToken("cookie-refresh-token");

        mockMvc.perform(post("/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "cookie-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authService).revokeRefreshToken("cookie-refresh-token");
    }

    @Test
    @DisplayName("POST /logout - Sans cookie, ne tente aucune révocation")
    void logout_WithoutCookie_DoesNotRevoke() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isNoContent());

        verify(authService, never()).revokeRefreshToken(anyString());
    }

    // ---------------------------------------------------------------
    // ACTIVATION
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /activate?token=... - Token valide, retourne les infos du compte")
    void checkActivationToken_ValidToken_ReturnsStatus() throws Exception {
        ActivationTokenStatusResponse resp = ActivationTokenStatusResponse.builder()
                .valid(true)
                .email("nouvel.agent@test.com")
                .firstName("Nouvel")
                .lastName("Agent")
                .build();

        when(authService.checkActivationToken("valid-token")).thenReturn(resp);

        mockMvc.perform(get("/activate").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("nouvel.agent@test.com"));
    }

    @Test
    @DisplayName("POST /activate - Active le compte avec un mot de passe valide")
    void activateAccount_Success() throws Exception {
        ActivateAccountRequest request = new ActivateAccountRequest();
        request.setToken("valid-token");
        request.setPassword("NouveauPass123");

        UserResponse activated = UserResponse.builder()
                .id(5L)
                .email("nouvel.agent@test.com")
                .status("ACTIVE")
                .build();

        when(authService.activateAccount(any())).thenReturn(activated);

        mockMvc.perform(post("/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /activate - Mot de passe trop court (< 8 caractères) -> 400")
    void activateAccount_PasswordTooShort_ReturnsBadRequest() throws Exception {
        ActivateAccountRequest request = new ActivateAccountRequest();
        request.setToken("valid-token");
        request.setPassword("short");

        mockMvc.perform(post("/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verify(authService, never()).activateAccount(any());
    }

    // ---------------------------------------------------------------
    // FORGOT / RESET PASSWORD
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /forgot-password - Retourne toujours 204, même si l'email n'existe pas (anti-enumeration)")
    void forgotPassword_ReturnsNoContent() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("inconnu@test.com");

        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /reset-password - Réinitialisation réussie")
    void resetPassword_ReturnsNoContent() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("NouveauPass123");

        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}