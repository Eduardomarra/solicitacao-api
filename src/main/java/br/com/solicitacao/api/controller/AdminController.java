package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.CoverageRequest;
import br.com.solicitacao.api.dto.request.CreateUserRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.api.dto.response.SolicitationResponse;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.service.AnalystService;
import br.com.solicitacao.core.service.AuthService;
import br.com.solicitacao.core.service.SolicitationService;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AnalystService analystService;
    private final AuthService authService;
    private final SolicitationRepository solicitationRepository;
    private final SolicitationService solicitationService;

    @PostMapping("/users")
    @Operation(summary = "Create user (ANALYST or ADMIN)")
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        AuthResponse response = authService.createUser(request);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<SolicitationResponse> getSolicitation(@PathVariable UUID id) {
        SolicitationEntity entity = solicitationService.findById(id);
        return ResponseEntity.ok(toResponse(entity));
    }

    private SolicitationListResponse toListResponse(SolicitationEntity entity) {
        return SolicitationListResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
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

    private SolicitationResponse toResponse(SolicitationEntity entity) {
        return SolicitationResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .status(entity.getStatus())
                .currentStep(entity.getCurrentStep())
                .serviceType(entity.getServiceType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .cep(entity.getCep())
                .number(entity.getNumber())
                .complement(entity.getComplement())
                .street(entity.getStreet())
                .neighborhood(entity.getNeighborhood())
                .city(entity.getCity())
                .state(entity.getState())
                .priority(entity.getPriority())
                .preferredDate(entity.getPreferredDate())
                .estimatedValue(entity.getEstimatedValue())
                .termsAccepted(entity.getTermsAccepted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .submittedAt(entity.getSubmittedAt())
                .analyzedAt(entity.getAnalyzedAt())
                .analyzedBy(entity.getAnalyzedBy())
                .analysisComment(entity.getAnalysisComment())
                .build();
    }
}