package com.portalcomunitario.mstenant.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba PlatformAuthController instanciándolo directamente con un JwtService mockeado
 * y credenciales sembradas (sin contexto Spring). Verifica el login de la credencial
 * de plataforma: correcta → token, incorrecta → 401.
 */
@ExtendWith(MockitoExtension.class)
class PlatformAuthControllerTest {

    private static final String EMAIL = "admin@plataforma.cl";
    private static final String PASSWORD = "s3creta";

    @Mock private JwtService jwtService;

    private PlatformAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new PlatformAuthController(jwtService, EMAIL, PASSWORD);
    }

    @Test
    @DisplayName("login: credencial correcta devuelve token, email y rol")
    void login_credencialCorrecta_devuelveToken() {
        when(jwtService.generarPlatformToken(EMAIL)).thenReturn("tok-plataforma");

        Map<String, String> res = controller.login(
                new PlatformAuthController.LoginRequest(EMAIL, PASSWORD));

        assertThat(res).containsEntry("token", "tok-plataforma");
        assertThat(res).containsEntry("email", EMAIL);
        assertThat(res).containsEntry("role", "PLATFORM_ADMIN");
    }

    @Test
    @DisplayName("login: email con mayúsculas/espacios igual autentica (case-insensitive y trim)")
    void login_emailNormalizado_autentica() {
        when(jwtService.generarPlatformToken(EMAIL)).thenReturn("tok-plataforma");

        Map<String, String> res = controller.login(
                new PlatformAuthController.LoginRequest("  Admin@Plataforma.CL  ", PASSWORD));

        assertThat(res).containsEntry("token", "tok-plataforma");
    }

    @Test
    @DisplayName("login: contraseña incorrecta lanza 401 y no genera token")
    void login_passwordIncorrecta_lanza401() {
        assertThatThrownBy(() -> controller.login(
                new PlatformAuthController.LoginRequest(EMAIL, "otra")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciales de plataforma inválidas");

        verify(jwtService, never()).generarPlatformToken(anyString());
    }

    @Test
    @DisplayName("login: email desconocido lanza 401")
    void login_emailDesconocido_lanza401() {
        assertThatThrownBy(() -> controller.login(
                new PlatformAuthController.LoginRequest("otro@x.cl", PASSWORD)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválidas");
    }

    @Test
    @DisplayName("login: request nulo lanza 401 (no NPE)")
    void login_requestNulo_lanza401() {
        assertThatThrownBy(() -> controller.login(null))
                .isInstanceOf(ResponseStatusException.class);
    }
}
