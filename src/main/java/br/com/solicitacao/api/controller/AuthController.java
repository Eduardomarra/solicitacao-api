package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.CreateUserRequest;
import br.com.solicitacao.api.dto.request.LoginRequest;
import br.com.solicitacao.api.dto.request.RegisterRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user (CLIENT)")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user (ANALYST or ADMIN) - Admin only")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        AuthResponse response = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}