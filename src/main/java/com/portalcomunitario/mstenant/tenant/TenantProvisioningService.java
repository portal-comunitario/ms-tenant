package com.portalcomunitario.mstenant.tenant;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.Statement;
import java.util.regex.Pattern;

@Service
public class TenantProvisioningService {

    private static final Pattern VALID_TENANT_ID = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    private final DataSourceConfig dataSourceConfig;
    private final TenantRoutingDataSource routingDataSource;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    public TenantProvisioningService(DataSourceConfig dataSourceConfig,
                                     TenantRoutingDataSource routingDataSource) {
        this.dataSourceConfig = dataSourceConfig;
        this.routingDataSource = routingDataSource;
    }

    public void provisionTenant(String tenantId) {
        validate(tenantId);
        createSchema(tenantId);
        // NOTA: el llenado de tablas (auth + community) se delega a esos servicios en MT2.
        // runMigrations(tenantId);  // <- ya no corre las migraciones propias de ms-tenant sobre el tenant

        DriverManagerDataSource tenantDataSource = dataSourceConfig.createDataSource(tenantId);
        routingDataSource.addDataSource(tenantId, tenantDataSource);
    }

    private void validate(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("El tenantId no puede ser nulo ni vacío");
        }
        if (!VALID_TENANT_ID.matcher(tenantId).matches()) {
            throw new IllegalArgumentException(
                    "tenantId inválido: '" + tenantId + "'. Debe iniciar con letra minúscula y " +
                    "contener solo minúsculas, dígitos o guion bajo (máx. 63 caracteres).");
        }
    }

    private void createSchema(String tenantId) {
        DriverManagerDataSource admin = dataSourceConfig.createDataSource("public");
        try (Connection conn = admin.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + tenantId);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear el schema '" + tenantId + "'", e);
        }
    }

    private void runMigrations(String tenantId) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(url, username, password)
                    .schemas(tenantId)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
        } catch (Exception e) {
            throw new RuntimeException("Falló la migración Flyway para el tenant '" + tenantId + "'", e);
        }
    }
}
