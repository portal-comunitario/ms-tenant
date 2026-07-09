package com.portalcomunitario.mstenant.comunidad;

import com.portalcomunitario.mstenant.tenant.TenantProvisioningService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Alta y administración de comunidades (tenants). */
@Service
public class ComunidadService {

    private final ComunidadRepository repo;
    private final TenantProvisioningService provisioning;
    private final ProvisioningClient provisioningClient;

    public ComunidadService(ComunidadRepository repo, TenantProvisioningService provisioning,
                            ProvisioningClient provisioningClient) {
        this.repo = repo;
        this.provisioning = provisioning;
        this.provisioningClient = provisioningClient;
    }

    public List<Comunidad> listar() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Comunidad crear(String nombre, String comuna, String adminEmail,
                           String sedeNombre, String sedeDireccion) {
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la comunidad es obligatorio");
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo del administrador es obligatorio");
        }
        if (sedeDireccion == null || sedeDireccion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dirección de la sede es obligatoria");
        }
        String slug = slugUnico(slugify(nombre));
        String codigo = generarCodigo(nombre);

        Comunidad c = new Comunidad();
        c.setNombre(nombre.trim());
        c.setComuna(comuna != null ? comuna.trim() : null);
        c.setSlug(slug);
        c.setCodigo(codigo);
        c.setAdminEmail(adminEmail.trim().toLowerCase());
        c.setSedeDireccion(sedeDireccion.trim());
        c.setSedeNombre(sedeNombre != null && !sedeNombre.isBlank()
                ? sedeNombre.trim() : "Sede " + nombre.trim());
        c.setEstado("ACTIVA");
        c = repo.save(c);

        // 1) Crea el schema. 2) Llena tablas de community + auth y crea el admin (en vivo).
        provisioning.provisionTenant(slug);
        provisioningClient.provisionarTodo(slug, c.getAdminEmail(), c.getCodigo());
        return c;
    }

    /** Edita metadata de la comunidad. El slug/schema NO cambia (es la identidad del tenant). */
    public Comunidad actualizar(UUID id, String nombre, String comuna, String adminEmail,
                                String sedeNombre, String sedeDireccion) {
        Comunidad c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunidad no encontrada"));
        if (nombre != null && !nombre.isBlank()) c.setNombre(nombre.trim());
        c.setComuna(comuna != null && !comuna.isBlank() ? comuna.trim() : null);
        if (sedeDireccion != null && !sedeDireccion.isBlank()) c.setSedeDireccion(sedeDireccion.trim());
        if (sedeNombre != null && !sedeNombre.isBlank()) c.setSedeNombre(sedeNombre.trim());

        boolean adminCambio = adminEmail != null && !adminEmail.isBlank()
                && !adminEmail.trim().equalsIgnoreCase(c.getAdminEmail());
        if (adminCambio) {
            c.setAdminEmail(adminEmail.trim().toLowerCase());
        }
        c = repo.save(c);

        // Si cambió el correo del admin, crea (idempotente) ese acceso en el schema con el código como clave temporal.
        if (adminCambio) {
            provisioningClient.crearAdmin(c.getSlug(), c.getAdminEmail(), c.getCodigo());
        }
        return c;
    }

    public Comunidad cambiarEstado(UUID id, String estado) {
        Comunidad c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunidad no encontrada"));
        String e = estado != null ? estado.trim().toUpperCase() : "";
        if (!e.equals("ACTIVA") && !e.equals("SUSPENDIDA")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido (ACTIVA o SUSPENDIDA)");
        }
        c.setEstado(e);
        return repo.save(c);
    }

    private String slugify(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.toLowerCase().trim().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (n.isBlank()) n = "comunidad";
        if (!Character.isLetter(n.charAt(0))) n = "c_" + n;
        if (n.length() > 60) n = n.substring(0, 60);
        return n;
    }

    private String slugUnico(String base) {
        String slug = base;
        int i = 2;
        while (repo.existsBySlug(slug)) {
            slug = base + "_" + i++;
        }
        return slug;
    }

    private String generarCodigo(String nombre) {
        StringBuilder iniciales = new StringBuilder();
        for (String w : nombre.trim().split("\\s+")) {
            if (!w.isBlank() && iniciales.length() < 4) iniciales.append(Character.toUpperCase(w.charAt(0)));
        }
        String pref = iniciales.length() > 0 ? iniciales.toString() : "COM";
        int rnd = ThreadLocalRandom.current().nextInt(1000, 10000);
        return pref + "-" + Year.now().getValue() + "-" + rnd;
    }
}
