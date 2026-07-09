package com.portalcomunitario.mstenant.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba TenantFilter resolviendo el tenant desde el header X-Tenant-ID, el subdominio
 * o el fallback "public", y verificando que el contexto se limpia al terminar la cadena.
 */
class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    /** Captura el tenant vigente DURANTE la ejecución de la cadena. */
    private String tenantDuranteCadena(HttpServletRequest req) throws Exception {
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        String[] holder = new String[1];
        doAnswer(inv -> { holder[0] = TenantContext.getCurrentTenant(); return null; })
                .when(chain).doFilter(req, res);
        filter.doFilterInternal(req, res, chain);
        return holder[0];
    }

    @Test
    @DisplayName("usa el header X-Tenant-ID cuando está presente y limpia al terminar")
    void usaElHeaderXTenantIdCuandoEstaPresente() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn("villa_los_aromos");

        assertThat(tenantDuranteCadena(req)).isEqualTo("villa_los_aromos");
        assertThat(TenantContext.getCurrentTenant()).isNull(); // se limpia al terminar
    }

    @Test
    @DisplayName("usa el subdominio cuando no hay header")
    void usaElSubdominioCuandoNoHayHeader() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn(null);
        when(req.getServerName()).thenReturn("villa.portal.cl");

        assertThat(tenantDuranteCadena(req)).isEqualTo("villa");
    }

    @Test
    @DisplayName("header en blanco cae al subdominio")
    void headerEnBlanco_caeAlSubdominio() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn("   ");
        when(req.getServerName()).thenReturn("jardin.portal.cl");

        assertThat(tenantDuranteCadena(req)).isEqualTo("jardin");
    }

    @Test
    @DisplayName("cae a 'public' cuando no hay header ni subdominio")
    void caeAPublicCuandoNoHayHeaderNiSubdominio() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn(null);
        when(req.getServerName()).thenReturn("localhost");

        assertThat(tenantDuranteCadena(req)).isEqualTo("public");
    }
}
