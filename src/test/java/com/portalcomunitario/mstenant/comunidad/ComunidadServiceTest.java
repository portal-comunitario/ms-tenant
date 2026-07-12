package com.portalcomunitario.mstenant.comunidad;

import com.portalcomunitario.mstenant.tenant.TenantProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComunidadServiceTest {

    @Mock private ComunidadRepository repo;
    @Mock private TenantProvisioningService provisioning;
    @Mock private ProvisioningClient provisioningClient;

    private ComunidadService service;

    @BeforeEach
    void setUp() {
        service = new ComunidadService(repo, provisioning, provisioningClient);
    }

    @Test
    @DisplayName("crear: genera slug, código e invoca el provisioning en vivo")
    void crear_generaSlugYCodigo() {
        when(repo.existsBySlug(anyString())).thenReturn(false);
        when(repo.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Comunidad c = service.crear("Villa Los Aromos", "Maipú", "Admin@Ejemplo.cl", "", "Av. Central 123, Maipú");

        assertThat(c.getSlug()).isEqualTo("villa_los_aromos");
        assertThat(c.getCodigo()).startsWith("VLA-");
        assertThat(c.getEstado()).isEqualTo("ACTIVA");
        assertThat(c.getAdminEmail()).isEqualTo("admin@ejemplo.cl");
        assertThat(c.getSedeDireccion()).isEqualTo("Av. Central 123, Maipú");
        assertThat(c.getSedeNombre()).isEqualTo("Sede Villa Los Aromos");
        verify(provisioning).provisionTenant("villa_los_aromos");
        verify(provisioningClient).provisionarTodo("villa_los_aromos", "admin@ejemplo.cl", c.getCodigo());
    }

    @Test
    @DisplayName("crear: el slug normaliza acentos (Jardín del Sur → jardin_del_sur)")
    void crear_slugSinAcentos() {
        when(repo.existsBySlug(anyString())).thenReturn(false);
        when(repo.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Comunidad c = service.crear("Jardín del Sur", "Maipú", "a@b.cl", "Sede JS", "Calle 1");

        assertThat(c.getSlug()).isEqualTo("jardin_del_sur");
    }

    @Test
    @DisplayName("crear: si el slug ya existe, agrega sufijo numérico")
    void crear_slugDuplicado_agregaSufijo() {
        when(repo.existsBySlug("villa_los_aromos")).thenReturn(true);
        when(repo.existsBySlug("villa_los_aromos_2")).thenReturn(false);
        when(repo.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Comunidad c = service.crear("Villa Los Aromos", null, "a@b.cl", null, "Calle 1");

        assertThat(c.getSlug()).isEqualTo("villa_los_aromos_2");
    }

    @Test
    @DisplayName("crear: rechaza nombre, correo o dirección de sede vacíos")
    void crear_camposObligatorios() {
        assertThatThrownBy(() -> service.crear("  ", "Maipú", "a@b.cl", null, "Calle 1"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("nombre");
        assertThatThrownBy(() -> service.crear("Villa", "Maipú", "  ", null, "Calle 1"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("administrador");
        assertThatThrownBy(() -> service.crear("Villa", "Maipú", "a@b.cl", null, "  "))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("sede");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("cambiarEstado: acepta ACTIVA/SUSPENDIDA y rechaza valores inválidos")
    void cambiarEstado() {
        UUID id = UUID.randomUUID();
        Comunidad c = new Comunidad();
        c.setEstado("ACTIVA");
        when(repo.findById(id)).thenReturn(Optional.of(c));
        when(repo.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Comunidad r = service.cambiarEstado(id, "suspendida");
        assertThat(r.getEstado()).isEqualTo("SUSPENDIDA");

        assertThatThrownBy(() -> service.cambiarEstado(id, "PAUSADA"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("actualizar: al cambiar el correo del admin, re-crea el acceso en el schema")
    void actualizar_cambioAdmin_reCreaAcceso() {
        UUID id = UUID.randomUUID();
        Comunidad c = new Comunidad();
        c.setSlug("villa_el_sol");
        c.setCodigo("VES-2026-1");
        c.setAdminEmail("old@ejemplo.cl");
        when(repo.findById(id)).thenReturn(Optional.of(c));
        when(repo.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Comunidad r = service.actualizar(id, "Nuevo Nombre", null, "New@Ejemplo.cl", null, "Nueva Dirección");

        assertThat(r.getNombre()).isEqualTo("Nuevo Nombre");
        assertThat(r.getAdminEmail()).isEqualTo("new@ejemplo.cl");
        assertThat(r.getSedeDireccion()).isEqualTo("Nueva Dirección");
        verify(provisioningClient).crearAdmin("villa_el_sol", "new@ejemplo.cl", "VES-2026-1");
    }

    @Test
    @DisplayName("actualizar: si el correo del admin no cambia, no re-crea el acceso")
    void actualizar_sinCambioAdmin_noReCrea() {
        UUID id = UUID.randomUUID();
        Comunidad c = new Comunidad();
        c.setSlug("villa_el_sol");
        c.setCodigo("VES-2026-1");
        c.setAdminEmail("admin@ejemplo.cl");
        when(repo.findById(id)).thenReturn(Optional.of(c));
        when(repo.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(id, "Otro Nombre", "Maipú", "admin@ejemplo.cl", null, "Dir");

        verify(provisioningClient, never()).crearAdmin(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("listar: devuelve las comunidades del repositorio")
    void listar_devuelve() {
        Comunidad c1 = new Comunidad(); c1.setNombre("A");
        Comunidad c2 = new Comunidad(); c2.setNombre("B");
        when(repo.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of(c1, c2));
        assertThat(service.listar()).hasSize(2);
    }

    @Test
    @DisplayName("cambiarEstado: comunidad inexistente lanza 404")
    void cambiarEstado_inexistente_404() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cambiarEstado(id, "ACTIVA"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("actualizar: comunidad inexistente lanza 404")
    void actualizar_inexistente_404() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizar(id, "N", null, null, null, "Dir"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("eliminar: si está SUSPENDIDA, deprovisiona el tenant y borra el registro")
    void eliminar_suspendida_deprovisionaYBorra() {
        UUID id = UUID.randomUUID();
        Comunidad c = new Comunidad();
        c.setSlug("villa_el_sol");
        c.setEstado("SUSPENDIDA");
        when(repo.findById(id)).thenReturn(Optional.of(c));

        service.eliminar(id);

        verify(provisioning).deprovisionTenant("villa_el_sol");
        verify(repo).delete(c);
    }

    @Test
    @DisplayName("eliminar: si está ACTIVA lanza 409 y no toca schema ni registro")
    void eliminar_activa_409() {
        UUID id = UUID.randomUUID();
        Comunidad c = new Comunidad();
        c.setSlug("villa_el_sol");
        c.setEstado("ACTIVA");
        when(repo.findById(id)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.eliminar(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("suspendida");
        verify(provisioning, never()).deprovisionTenant(anyString());
        verify(repo, never()).delete(any(Comunidad.class));
    }

    @Test
    @DisplayName("eliminar: comunidad inexistente lanza 404")
    void eliminar_inexistente_404() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.eliminar(id))
                .isInstanceOf(ResponseStatusException.class);
        verify(provisioning, never()).deprovisionTenant(anyString());
    }
}
