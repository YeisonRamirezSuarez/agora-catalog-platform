package com.agora.catalog.auth.controller;

import com.agora.catalog.auth.dto.request.ChangePasswordRequest;
import com.agora.catalog.auth.dto.request.LoginRequest;
import com.agora.catalog.auth.dto.request.RefreshTokenRequest;
import com.agora.catalog.auth.dto.response.LoginResponse;
import com.agora.catalog.auth.model.UsuarioPrincipal;
import com.agora.catalog.auth.service.ChangePasswordService;
import com.agora.catalog.auth.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginService loginService;
    private final ChangePasswordService changePasswordService;

    public AuthController(LoginService loginService, ChangePasswordService changePasswordService) {
        this.loginService = loginService;
        this.changePasswordService = changePasswordService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return loginService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        loginService.logout(request.refreshToken());
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UsuarioPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordService.change(principal.id(), request.actual(), request.nueva());
    }
}
