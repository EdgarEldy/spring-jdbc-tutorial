package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.auth.service.RbacService;
import com.edgareldy.springjdbctutorial.ws.payload.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import com.edgareldy.springjdbctutorial.ws.converter.UserConverter;
import com.edgareldy.springjdbctutorial.ws.payload.auth.UserResponse;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry points to list users and assign or remove their roles. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @PreAuthorize is evaluated by the method security proxy around this controller (see SecurityConfig):
// hasPermission('X','Y') is answered by CustomPermissionEvaluator from the JWT authorities.
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final RbacService rbacService;

    public UserController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    // @Min/@Max on request parameters are checked by Spring MVC 6.1 built-in method validation before the
    // call; a violation raises HandlerMethodValidationException, mapped to 400 by GlobalExceptionHandler.
    @GetMapping
    @PreAuthorize("hasPermission('USER','READ')")
    public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(UserConverter.toPageResponse(rbacService.listUsers(page, size)), "Users");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('USER','READ')")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.success(UserConverter.toResponse(rbacService.getUser(id)), "User");
    }

    @PatchMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasPermission('USER','WRITE')")
    public ApiResponse<UserResponse> assignRole(@PathVariable Long id, @PathVariable Long roleId) {
        return ApiResponse.success(UserConverter.toResponse(rbacService.assignRoleToUser(id, roleId)), "Role assigned");
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasPermission('USER','WRITE')")
    public ApiResponse<UserResponse> removeRole(@PathVariable Long id, @PathVariable Long roleId) {
        return ApiResponse.success(UserConverter.toResponse(rbacService.removeRoleFromUser(id, roleId)), "Role removed");
    }
}
