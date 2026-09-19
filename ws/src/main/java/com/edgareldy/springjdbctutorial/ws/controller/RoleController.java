package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.auth.service.RbacService;
import com.edgareldy.springjdbctutorial.ws.payload.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import com.edgareldy.springjdbctutorial.ws.converter.RoleConverter;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.RoleRequest;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.RoleResponse;
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
 * HTTP entry points to manage roles and the permissions assigned onto them. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @PreAuthorize is evaluated by the method security proxy around this controller (see SecurityConfig):
// hasPermission('X','Y') is answered by CustomPermissionEvaluator from the JWT authorities.
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RbacService rbacService;

    public RoleController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('ROLE','READ')")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.success(RoleConverter.toResponses(rbacService.listRoles()), "Roles");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('ROLE','WRITE')")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.success(RoleConverter.toResponse(rbacService.createRole(RoleConverter.toDto(request))), "Role created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('ROLE','WRITE')")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.success(RoleConverter.toResponse(rbacService.updateRole(id, RoleConverter.toDto(request))), "Role updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('ROLE','WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return ApiResponse.success(null, "Role deleted");
    }

    @PostMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasPermission('ROLE','WRITE')")
    public ApiResponse<RoleResponse> assignPermission(@PathVariable Long id, @PathVariable Long permissionId) {
        return ApiResponse.success(RoleConverter.toResponse(rbacService.assignPermissionToRole(id, permissionId)), "Permission assigned");
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasPermission('ROLE','WRITE')")
    public ApiResponse<RoleResponse> removePermission(@PathVariable Long id, @PathVariable Long permissionId) {
        return ApiResponse.success(RoleConverter.toResponse(rbacService.removePermissionFromRole(id, permissionId)), "Permission removed");
    }
}
