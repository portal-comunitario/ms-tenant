package com.portalcomunitario.mstenant.comunidad;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Panel de plataforma: alta y administración de comunidades (solo PLATFORM_ADMIN). */
@RestController
@RequestMapping("/platform/comunidades")
public class ComunidadController {

    private final ComunidadService service;

    public ComunidadController(ComunidadService service) {
        this.service = service;
    }

    @PostMapping
    public ComunidadDto crear(@RequestBody CrearRequest req, Authentication auth) {
        requirePlatformAdmin(auth);
        return dto(service.crear(req.nombre(), req.comuna(), req.adminEmail(),
                req.sedeNombre(), req.sedeDireccion()));
    }

    @GetMapping
    public List<ComunidadDto> listar(Authentication auth) {
        requirePlatformAdmin(auth);
        return service.listar().stream().map(this::dto).toList();
    }

    @PutMapping("/{id}")
    public ComunidadDto actualizar(@PathVariable UUID id, @RequestBody EditarRequest req, Authentication auth) {
        requirePlatformAdmin(auth);
        return dto(service.actualizar(id, req.nombre(), req.comuna(), req.adminEmail(),
                req.sedeNombre(), req.sedeDireccion()));
    }

    @PutMapping("/{id}/estado")
    public ComunidadDto cambiarEstado(@PathVariable UUID id, @RequestBody EstadoRequest req, Authentication auth) {
        requirePlatformAdmin(auth);
        return dto(service.cambiarEstado(id, req.estado()));
    }

    private void requirePlatformAdmin(Authentication auth) {
        String role = "";
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            role = jwt.getClaimAsString("role");
        }
        if (!"PLATFORM_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el administrador de plataforma puede gestionar comunidades");
        }
    }

    private ComunidadDto dto(Comunidad c) {
        return new ComunidadDto(c.getId(), c.getNombre(), c.getComuna(), c.getSlug(), c.getCodigo(),
                c.getAdminEmail(), c.getEstado(), "/c/" + c.getSlug(),
                c.getSedeNombre(), c.getSedeDireccion());
    }

    public record CrearRequest(String nombre, String comuna, String adminEmail,
                               String sedeNombre, String sedeDireccion) {}
    public record EditarRequest(String nombre, String comuna, String adminEmail,
                                String sedeNombre, String sedeDireccion) {}
    public record EstadoRequest(String estado) {}
    public record ComunidadDto(UUID id, String nombre, String comuna, String slug, String codigo,
                               String adminEmail, String estado, String url,
                               String sedeNombre, String sedeDireccion) {}
}
