package com.portalcomunitario.mstenant.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Login del administrador de plataforma (credencial sembrada — demo). */
@RestController
@RequestMapping("/platform")
public class PlatformAuthController {

    private final JwtService jwtService;
    private final String platformEmail;
    private final String platformPassword;

    public PlatformAuthController(JwtService jwtService,
                                 @Value("${app.platform.email}") String platformEmail,
                                 @Value("${app.platform.password}") String platformPassword) {
        this.jwtService = jwtService;
        this.platformEmail = platformEmail;
        this.platformPassword = platformPassword;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        String email = req != null && req.email() != null ? req.email().trim() : "";
        String pass = req != null && req.password() != null ? req.password() : "";
        if (!platformEmail.equalsIgnoreCase(email) || !platformPassword.equals(pass)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales de plataforma inválidas");
        }
        return Map.of(
                "token", jwtService.generarPlatformToken(platformEmail),
                "email", platformEmail,
                "role", "PLATFORM_ADMIN");
    }

    public record LoginRequest(String email, String password) {}
}
