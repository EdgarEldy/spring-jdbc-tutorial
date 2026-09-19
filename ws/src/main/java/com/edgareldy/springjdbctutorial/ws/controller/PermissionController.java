package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.auth.service.RbacService;
import com.edgareldy.springjdbctutorial.ws.payload.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import com.edgareldy.springjdbctutorial.ws.converter.PermissionConverter;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.PermissionRequest;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.PermissionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry points to manage permissions. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @PreAuthorize is evaluated by the method security proxy around this controller (see SecurityConfig):
// hasPermission('X','Y') is answered by CustomPermissionEvaluator from the JWT authorities.
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final RbacService rbacService;

    public PermissionController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('PERMISSION','READ')")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.success(PermissionConverter.toResponses(rbacService.listPermissions()), "Permissions");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('PERMISSION','WRITE')")
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
        return ApiResponse.success(PermissionConverter.toResponse(rbacService.createPermission(PermissionConverter.toDto(request))), "Permission created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('PERMISSION','WRITE')")
    public ApiResponse<PermissionResponse> update(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        return ApiResponse.success(PermissionConverter.toResponse(rbacService.updatePermission(id, PermissionConverter.toDto(request))), "Permission updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('PERMISSION','WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        rbacService.deletePermission(id);
        return ApiResponse.success(null, "Permission deleted");
    }
}
