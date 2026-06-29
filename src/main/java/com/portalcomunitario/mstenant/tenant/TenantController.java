package com.portalcomunitario.mstenant.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantProvisioningService provisioningService;

    public TenantController(TenantProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody TenantRegistrationRequest request) {
        String tenantId = request != null ? request.tenantId() : null;
        provisioningService.provisionTenant(tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "created",
                "tenantId", tenantId,
                "message", "Tenant '" + tenantId + "' registrado correctamente"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTenant(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", ex.getMessage()
        ));
    }

    public record TenantRegistrationRequest(String tenantId) {}
}
