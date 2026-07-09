package com.portalcomunitario.mstenant.comunidad;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Metadata pública de una comunidad, para que el portal se identifique (nombre, comuna, sede).
 * No expone datos sensibles (código de admin, correo del admin).
 */
@RestController
@RequestMapping("/public/comunidades")
public class PublicComunidadController {

    private final ComunidadRepository repo;

    public PublicComunidadController(ComunidadRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{slug}")
    public PublicDto porSlug(@PathVariable String slug) {
        Comunidad c = repo.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunidad no encontrada"));
        if (!"ACTIVA".equals(c.getEstado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunidad no disponible");
        }
        return new PublicDto(c.getNombre(), c.getComuna(), c.getSlug(),
                c.getSedeNombre(), c.getSedeDireccion());
    }

    public record PublicDto(String nombre, String comuna, String slug,
                            String sedeNombre, String sedeDireccion) {}
}
