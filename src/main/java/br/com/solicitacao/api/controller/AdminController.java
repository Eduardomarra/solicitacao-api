package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.CoverageRequest;
import br.com.solicitacao.api.dto.request.CreateUserRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.core.service.AnalystService;
import br.com.solicitacao.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Endpoints for administrators")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AnalystService analystService;
    private final AuthService authService;

    @PostMapping("/users")
    @Operation(summary = "Create user (ANALYST or ADMIN)")
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        AuthResponse response = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/analysts/{userId}/coverage")
    @Operation(summary = "Update analyst state coverage")
    public ResponseEntity<Void> updateAnalystCoverage(
            @PathVariable UUID userId,
            @Valid @RequestBody CoverageRequest request) {
        analystService.updateCoverage(userId, request.getUfs());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analysts/{userId}/coverage")
    @Operation(summary = "Get analyst state coverage")
    public ResponseEntity<List<String>> getAnalystCoverage(@PathVariable UUID userId) {
        List<String> states = analystService.getAnalystStates(userId);
        return ResponseEntity.ok(states);
    }
}