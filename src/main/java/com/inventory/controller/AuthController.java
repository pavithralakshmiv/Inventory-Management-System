package com.inventory.controller;

import com.inventory.dto.MeResponse;
import com.inventory.dto.PasswordChangeRequest;
import com.inventory.dto.ProfileRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Map.of("message", "Registration successful. You can now log in.");
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me();
    }

    @PutMapping("/profile")
    public MeResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        return authService.updateProfile(request);
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return Map.of("message", "Password changed successfully");
    }
}
