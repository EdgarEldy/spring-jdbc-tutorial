package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.auth.dto.BlacklistedTokenDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.ws.converter.UserConverter;
import com.edgareldy.springjdbctutorial.ws.payload.auth.ForgotPasswordRequest;
import com.edgareldy.springjdbctutorial.ws.payload.auth.LoginRequest;
import com.edgareldy.springjdbctutorial.ws.payload.auth.LoginResponse;
import com.edgareldy.springjdbctutorial.ws.payload.auth.RegisterRequest;
import com.edgareldy.springjdbctutorial.ws.payload.auth.ResetPasswordRequest;
import com.edgareldy.springjdbctutorial.ws.payload.auth.UserResponse;
import com.edgareldy.springjdbctutorial.ws.payload.common.ApiResponse;
import com.edgareldy.springjdbctutorial.ws.security.AuthenticatedUser;
import com.edgareldy.springjdbctutorial.ws.security.IssuedToken;
import com.edgareldy.springjdbctutorial.ws.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry points of registration, activation, login, logout, profile and password reset. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @AuthenticationPrincipal injects the principal the JwtAuthFilter put in the SecurityContext.
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserDto created = authService.register(UserConverter.toDto(request));
        return ApiResponse.success(UserConverter.toResponse(created), "Account created, activation required");
    }

    @GetMapping("/activate-account")
    public ApiResponse<Void> activateAccount(@RequestParam("token") String token) {
        authService.activateAccount(token);
        return ApiResponse.success(null, "Account activated");
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserDto user = authService.login(request.getEmail(), request.getPassword());
        IssuedToken issued = jwtService.issue(user);
        return ApiResponse.success(new LoginResponse(issued.getToken(), "Bearer", issued.getExpiresAt()), "Login successful");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        authService.logout(new BlacklistedTokenDto(principal.getUserId(), principal.getTokenHash(),
                principal.getJti(), principal.getIssuedAt(), principal.getExpiresAt()));
        return ApiResponse.success(null, "Logged out");
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(UserConverter.toResponse(authService.getProfile(principal.getUserId())), "Profile");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ApiResponse.success(null, "If the email is registered, a reset link has been generated");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success(null, "Password reset");
    }
}
