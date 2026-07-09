package com.portalcomunitario.mstenant.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba el ThreadLocal de tenant: set/get/clear. */
class TenantContextTest {

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("set/get: guarda y devuelve el tenant actual")
    void guardaYDevuelveElTenantActual() {
        TenantContext.setCurrentTenant("villa_los_aromos");
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("villa_los_aromos");
    }

    @Test
    @DisplayName("clear: deja el contexto nulo")
    void clearDejaElContextoNulo() {
        TenantContext.setCurrentTenant("villa_los_aromos");
        TenantContext.clear();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("sin asignar: devuelve nulo")
    void sinAsignar_devuelveNulo() {
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
