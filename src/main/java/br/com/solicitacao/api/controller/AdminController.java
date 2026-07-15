package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.CoverageRequest;
import br.com.solicitacao.api.dto.request.CreateUserRequest;
import br.com.solicitacao.api.dto.request.UpdateUserRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.exception.ResourceNotFoundException;
import br.com.solicitacao.core.service.AnalystService;
import br.com.solicitacao.core.service.AuthService;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Endpoints for administrators")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final SolicitationRepository solicitationRepository;
    private final AnalystService analystService;
    private final AuthService authService;

    // ============================================
    // USERS
    // ============================================

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserEntity>> getUsers() {
        List<UserEntity> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserEntity> getUser(@PathVariable UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    @Operation(summary = "Create user (ANALYST or ADMIN)")
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        AuthResponse response = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<UserEntity> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        UserEntity updated = userRepository.save(user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // ANALYST COVERAGE
    // ============================================

    @PutMapping("/analysts/{userId}/coverage")
    @Operation(summary = "Update analyst state coverage")
    public ResponseEntity<Void> updateAnalystCoverage(
            @PathVariable UUID userId,
            @Valid @RequestBody CoverageRequest request) {
        analystService.updateCoverage(userId, request.getStates());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analysts/{userId}/coverage")
    @Operation(summary = "Get analyst state coverage")
    public ResponseEntity<List<String>> getAnalystCoverage(@PathVariable UUID userId) {
        List<String> states = analystService.getAnalystStates(userId);
        return ResponseEntity.ok(states);
    }

    // ============================================
    // SOLICITATIONS
    // ============================================

    @GetMapping("/solicitations")
    @Operation(summary = "List all solicitations (Admin only)")
    public ResponseEntity<Page<SolicitationListResponse>> listAllSolicitations(
            @RequestParam(required = false) SolicitationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SolicitationEntity> entities;
        if (status != null) {
            entities = solicitationRepository.findByStatus(status, pageable);
        } else {
            entities = solicitationRepository.findAll(pageable);
        }

        Page<SolicitationListResponse> response = entities.map(this::toListResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/solicitations/{id}")
    @Operation(summary = "Get solicitation by ID (Admin only)")
    public ResponseEntity<SolicitationListResponse> getSolicitation(@PathVariable UUID id) {
        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));
        return ResponseEntity.ok(toListResponse(entity));
    }

    // ============================================
    // DASHBOARD STATS
    // ============================================

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalSolicitations", solicitationRepository.count());
        stats.put("analysts", userRepository.countByRole(Role.ANALYST));
        stats.put("clients", userRepository.countByRole(Role.CLIENT));
        stats.put("admins", userRepository.countByRole(Role.ADMIN));
        stats.put("pendingSolicitations", solicitationRepository.countByStatus(SolicitationStatus.SUBMITTED));
        stats.put("approvedSolicitations", solicitationRepository.countByStatus(SolicitationStatus.APPROVED));
        stats.put("rejectedSolicitations", solicitationRepository.countByStatus(SolicitationStatus.REJECTED));
        stats.put("inReviewSolicitations", solicitationRepository.countByStatus(SolicitationStatus.IN_REVIEW));
        return ResponseEntity.ok(stats);
    }

    // ============================================
    // AUXILIAR METHODS
    // ============================================

    private SolicitationListResponse toListResponse(SolicitationEntity entity) {
        String clientName = userRepository.findById(entity.getClientId())
                .map(UserEntity::getName)
                .orElse("Usuário não encontrado");

        return SolicitationListResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .status(entity.getStatus())
                .currentStep(entity.getCurrentStep())
                .serviceType(entity.getServiceType())
                .title(entity.getTitle())
                .city(entity.getCity())
                .state(entity.getState())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}