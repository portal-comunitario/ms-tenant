package com.portalcomunitario.mstenant.comunidad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba ComunidadController instanciándolo directamente con un ComunidadService
 * mockeado (sin contexto Spring). Cubre la delegación de cada endpoint, el mapeo a
 * ComunidadDto y el guard requirePlatformAdmin (rol PLATFORM_ADMIN vs. otros → 403).
 */
@ExtendWith(MockitoExtension.class)
class ComunidadControllerTest {

    @Mock private ComunidadService service;

    private ComunidadController controller;

    @BeforeEach
    void setUp() {
        controller = new ComunidadController(service);
    }

    /** Authentication real de tipo JwtAuthenticationToken con el claim "role". */
    private JwtAuthenticationToken authConRol(String role) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    private Comunidad comunidad(String nombre, String slug, String estado) {
        Comunidad c = new Comunidad();
        c.setNombre(nombre);
        c.setComuna("Maipú");
        c.setSlug(slug);
        c.setCodigo("VLA-2026-1");
        c.setAdminEmail("admin@ejemplo.cl");
        c.setEstado(estado);
        c.setSedeNombre("Sede " + nombre);
        c.setSedeDireccion("Av. Central 123");
        return c;
    }

    // ---------- crear ----------

    @Test
    @DisplayName("crear: admin de plataforma delega en el service y mapea a ComunidadDto")
    void crear_admin_delegaYMapea() {
        Comunidad c = comunidad("Villa Los Aromos", "villa_los_aromos", "ACTIVA");
        when(service.crear("Villa Los Aromos", "Maipú", "admin@ejemplo.cl", "Sede X", "Av. Central 123"))
                .thenReturn(c);

        ComunidadController.ComunidadDto dto = controller.crear(
                new ComunidadController.CrearRequest(
                        "Villa Los Aromos", "Maipú", "admin@ejemplo.cl", "Sede X", "Av. Central 123"),
                authConRol("PLATFORM_ADMIN"));

        assertThat(dto.nombre()).isEqualTo("Villa Los Aromos");
        assertThat(dto.slug()).isEqualTo("villa_los_aromos");
        assertThat(dto.estado()).isEqualTo("ACTIVA");
        assertThat(dto.url()).isEqualTo("/c/villa_los_aromos");
        assertThat(dto.sedeNombre()).isEqualTo("Sede Villa Los Aromos");
    }

    @Test
    @DisplayName("crear: un rol distinto de PLATFORM_ADMIN recibe 403 y no toca el service")
    void crear_rolNoAutorizado_lanza403() {
        assertThatThrownBy(() -> controller.crear(
                new ComunidadController.CrearRequest("X", null, "a@b.cl", null, "Calle 1"),
                authConRol("VECINO")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrador de plataforma");

        verifyNoInteractions(service);
    }

    // ---------- listar ----------

    @Test
    @DisplayName("listar: admin obtiene la lista mapeada a ComunidadDto")
    void listar_admin_devuelveLista() {
        when(service.listar()).thenReturn(List.of(
                comunidad("Villa Los Aromos", "villa_los_aromos", "ACTIVA"),
                comunidad("Jardín del Sur", "jardin_del_sur", "SUSPENDIDA")));

        List<ComunidadController.ComunidadDto> dtos = controller.listar(authConRol("PLATFORM_ADMIN"));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).slug()).isEqualTo("villa_los_aromos");
        assertThat(dtos.get(1).estado()).isEqualTo("SUSPENDIDA");
    }

    // ---------- actualizar ----------

    @Test
    @DisplayName("actualizar: admin delega en el service con los campos del request")
    void actualizar_admin_delega() {
        UUID id = UUID.randomUUID();
        Comunidad c = comunidad("Nuevo Nombre", "villa_los_aromos", "ACTIVA");
        when(service.actualizar(id, "Nuevo Nombre", "Maipú", "new@ejemplo.cl", "Sede N", "Nueva Dir"))
                .thenReturn(c);

        ComunidadController.ComunidadDto dto = controller.actualizar(
                id,
                new ComunidadController.EditarRequest(
                        "Nuevo Nombre", "Maipú", "new@ejemplo.cl", "Sede N", "Nueva Dir"),
                authConRol("PLATFORM_ADMIN"));

        assertThat(dto.nombre()).isEqualTo("Nuevo Nombre");
    }

    // ---------- cambiarEstado ----------

    @Test
    @DisplayName("cambiarEstado: admin delega en el service y devuelve el nuevo estado")
    void cambiarEstado_admin_delega() {
        UUID id = UUID.randomUUID();
        Comunidad c = comunidad("Villa Los Aromos", "villa_los_aromos", "SUSPENDIDA");
        when(service.cambiarEstado(id, "SUSPENDIDA")).thenReturn(c);

        ComunidadController.ComunidadDto dto = controller.cambiarEstado(
                id, new ComunidadController.EstadoRequest("SUSPENDIDA"), authConRol("PLATFORM_ADMIN"));

        assertThat(dto.estado()).isEqualTo("SUSPENDIDA");
    }

    // ---------- eliminar ----------

    @Test
    @DisplayName("eliminar: admin delega el borrado en el service")
    void eliminar_admin_delega() {
        UUID id = UUID.randomUUID();

        controller.eliminar(id, authConRol("PLATFORM_ADMIN"));

        verify(service).eliminar(id);
    }

    @Test
    @DisplayName("eliminar: un rol distinto de PLATFORM_ADMIN recibe 403 y no toca el service")
    void eliminar_rolNoAutorizado_lanza403() {
        assertThatThrownBy(() -> controller.eliminar(UUID.randomUUID(), authConRol("VECINO")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrador de plataforma");

        verifyNoInteractions(service);
    }

    // ---------- guard sin token JWT ----------

    @Test
    @DisplayName("requirePlatformAdmin: una Authentication que no es JWT recibe 403")
    void guard_authNoJwt_lanza403() {
        assertThatThrownBy(() -> controller.listar(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrador de plataforma");

        verifyNoInteractions(service);
    }
}
