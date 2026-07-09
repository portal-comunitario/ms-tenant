package com.portalcomunitario.mstenant.platform;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba JwtService instanciándolo directamente con un secreto y expiración fijos
 * (sin contexto Spring). Verifica que produce un token firmado no vacío y con los
 * claims esperados (subject, email, role=PLATFORM_ADMIN).
 */
class JwtServiceTest {

    // 64 bytes → clave apta para HS512.
    private static final String SECRET = "0123456789012345678901234567890123456789012345678901234567890123";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generarPlatformToken: produce un token no vacío con tres segmentos")
    void generarPlatformToken_tokenNoVacio() {
        String token = jwtService.generarPlatformToken("admin@plataforma.cl");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("generarPlatformToken: los claims incluyen subject, email y rol PLATFORM_ADMIN")
    void generarPlatformToken_claimsCorrectos() {
        String token = jwtService.generarPlatformToken("admin@plataforma.cl");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("admin@plataforma.cl");
        assertThat(claims.get("email", String.class)).isEqualTo("admin@plataforma.cl");
        assertThat(claims.get("role", String.class)).isEqualTo("PLATFORM_ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
