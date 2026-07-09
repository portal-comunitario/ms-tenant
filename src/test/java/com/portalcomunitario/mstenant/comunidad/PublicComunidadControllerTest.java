package com.portalcomunitario.mstenant.comunidad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Prueba PublicComunidadController instanciándolo directamente con el repositorio
 * mockeado (sin contexto Spring). Verifica: ACTIVA → dto sin datos sensibles,
 * inexistente → 404, y no ACTIVA → 404.
 */
@ExtendWith(MockitoExtension.class)
class PublicComunidadControllerTest {

    @Mock private ComunidadRepository repo;

    private PublicComunidadController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicComunidadController(repo);
    }

    private Comunidad comunidad(String estado) {
        Comunidad c = new Comunidad();
        c.setNombre("Villa Los Aromos");
        c.setComuna("Maipú");
        c.setSlug("villa_los_aromos");
        c.setCodigo("VLA-2026-1");
        c.setAdminEmail("admin@ejemplo.cl");
        c.setEstado(estado);
        c.setSedeNombre("Sede Villa Los Aromos");
        c.setSedeDireccion("Av. Central 123");
        return c;
    }

    @Test
    @DisplayName("porSlug: comunidad ACTIVA devuelve la metadata pública (sin datos sensibles)")
    void porSlug_activa_devuelveDto() {
        when(repo.findBySlug("villa_los_aromos")).thenReturn(Optional.of(comunidad("ACTIVA")));

        PublicComunidadController.PublicDto dto = controller.porSlug("villa_los_aromos");

        assertThat(dto.nombre()).isEqualTo("Villa Los Aromos");
        assertThat(dto.comuna()).isEqualTo("Maipú");
        assertThat(dto.slug()).isEqualTo("villa_los_aromos");
        assertThat(dto.sedeNombre()).isEqualTo("Sede Villa Los Aromos");
        assertThat(dto.sedeDireccion()).isEqualTo("Av. Central 123");
    }

    @Test
    @DisplayName("porSlug: comunidad inexistente lanza 404")
    void porSlug_inexistente_lanza404() {
        when(repo.findBySlug("no_existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.porSlug("no_existe"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("porSlug: comunidad no ACTIVA (suspendida) lanza 404")
    void porSlug_noActiva_lanza404() {
        when(repo.findBySlug("villa_los_aromos")).thenReturn(Optional.of(comunidad("SUSPENDIDA")));

        assertThatThrownBy(() -> controller.porSlug("villa_los_aromos"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no disponible");
    }
}
