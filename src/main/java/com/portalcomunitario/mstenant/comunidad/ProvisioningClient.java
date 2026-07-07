package com.portalcomunitario.mstenant.comunidad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Orquesta el provisioning del schema de un tenant llamando a ms-community y ms-auth. */
@Component
public class ProvisioningClient {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningClient.class);

    private final RestClient http = RestClient.create();
    private final String authUrl;
    private final String communityUrl;
    private final String token;

    public ProvisioningClient(@Value("${app.services.auth-url:http://localhost:8081}") String authUrl,
                              @Value("${app.services.community-url:http://localhost:8084}") String communityUrl,
                              @Value("${app.internal.token:portal-internal}") String token) {
        this.authUrl = authUrl;
        this.communityUrl = communityUrl;
        this.token = token;
    }

    /** Provisiona las tablas de community y auth en el schema, y crea el admin de la comunidad. */
    public void provisionarTodo(String slug, String adminEmail, String adminPassword) {
        provision(communityUrl, slug);
        provision(authUrl, slug);
        crearAdmin(slug, adminEmail, adminPassword);
        log.info("Tenant '{}' provisionado end-to-end (community + auth + admin)", slug);
    }

    private void provision(String baseUrl, String slug) {
        http.post()
                .uri(baseUrl + "/internal/tenants/" + slug + "/provision")
                .header("X-Internal-Token", token)
                .retrieve()
                .toBodilessEntity();
    }

    private void crearAdmin(String slug, String adminEmail, String adminPassword) {
        http.post()
                .uri(authUrl + "/internal/tenants/" + slug + "/admin")
                .header("X-Internal-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", adminEmail, "nombre", "Administrador", "password", adminPassword))
                .retrieve()
                .toBodilessEntity();
    }
}
