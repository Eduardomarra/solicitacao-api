package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.*;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.api.dto.response.SolicitationResponse;
import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.core.exception.ResourceNotFoundException;
import br.com.solicitacao.core.service.AnalystService;
import br.com.solicitacao.core.service.AuthService;
import br.com.solicitacao.core.service.SolicitationService;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Endpoints for administrators")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final SolicitationRepository solicitationRepository;
    private final SolicitationService solicitationService;
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
    // SOLICITATIONS - ADMIN
    // ============================================

    @GetMapping("/solicitations")
    @Operation(summary = "List all solicitations with filters")
    public ResponseEntity<Page<SolicitationListResponse>> listAllSolicitations(
            @RequestParam(required = false) SolicitationStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String state,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SolicitationEntity> entities;

        if (status != null && clientId != null) {
            entities = solicitationRepository.findByClientIdAndStatus(clientId, status, pageable);
        } else if (status != null) {
            entities = solicitationRepository.findByStatus(status, pageable);
        } else if (clientId != null) {
            entities = solicitationRepository.findByClientId(clientId, pageable);
        } else if (state != null) {
            entities = solicitationRepository.findByState(state, pageable);
        } else {
            entities = solicitationRepository.findAll(pageable);
        }

        Page<SolicitationListResponse> response = entities.map(this::toListResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/solicitations/{id}")
    @Operation(summary = "Get solicitation by ID (Admin only)")
    public ResponseEntity<SolicitationResponse> getSolicitation(@PathVariable UUID id) {
        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));
        return ResponseEntity.ok(toResponse(entity));
    }

    @PostMapping("/solicitations")
    @Operation(summary = "Create a new solicitation as ADMIN")
    public ResponseEntity<SolicitationResponse> createSolicitationAsAdmin(
            @Valid @RequestBody AdminCreateSolicitationRequest request) {

        log.info("=== ADMIN CREATING SOLICITATION ===");
        log.info("Request: {}", request);

        UserEntity client = userRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        SolicitationEntity solicitation = solicitationService.create(request.getClientId());

        if (request.getServiceType() != null && request.getTitle() != null && request.getDescription() != null) {
            SolicitationStep1Request step1 = SolicitationStep1Request.builder()
                    .serviceType(request.getServiceType())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .build();
            solicitation = solicitationService.saveStep1(solicitation.getId(), request.getClientId(), step1);
        }

        if (request.getCep() != null && request.getNumber() != null) {
            SolicitationStep2Request step2 = SolicitationStep2Request.builder()
                    .cep(request.getCep())
                    .number(request.getNumber())
                    .complement(request.getComplement())
                    .street(request.getStreet())
                    .neighborhood(request.getNeighborhood())
                    .city(request.getCity())
                    .state(request.getState())
                    .build();
            solicitation = solicitationService.saveStep2(solicitation.getId(), request.getClientId(), step2);
        }

        if (request.getPriority() != null && request.getPreferredDate() != null) {
            SolicitationStep3Request step3 = SolicitationStep3Request.builder()
                    .priority(request.getPriority())
                    .preferredDate(request.getPreferredDate())
                    .estimatedValue(request.getEstimatedValue() != null ? request.getEstimatedValue() : BigDecimal.ZERO)
                    .termsAccepted(request.getTermsAccepted() != null ? request.getTermsAccepted() : true)
                    .build();
            solicitation = solicitationService.saveStep3(solicitation.getId(), request.getClientId(), step3);
        }

        if (Boolean.TRUE.equals(request.getSubmitDirectly())) {
            solicitation = solicitationService.submit(solicitation.getId(), request.getClientId());
        }

        log.info("ADMIN created solicitation: {}", solicitation.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(solicitation));
    }

    @PutMapping("/solicitations/{id}")
    @Operation(summary = "Update any solicitation as ADMIN")
    public ResponseEntity<SolicitationResponse> updateSolicitationAsAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateSolicitationRequest request) {

        log.info("=== ADMIN UPDATING SOLICITATION ===");
        log.info("Solicitation ID: {}", id);

        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        UUID clientId = entity.getClientId();

        if (request.getServiceType() != null || request.getTitle() != null || request.getDescription() != null) {
            SolicitationStep1Request step1 = SolicitationStep1Request.builder()
                    .serviceType(request.getServiceType() != null ? request.getServiceType() : entity.getServiceType())
                    .title(request.getTitle() != null ? request.getTitle() : entity.getTitle())
                    .description(request.getDescription() != null ? request.getDescription() : entity.getDescription())
                    .build();
            entity = solicitationService.saveStep1(id, clientId, step1);
        }

        if (request.getCep() != null || request.getNumber() != null) {
            SolicitationStep2Request step2 = SolicitationStep2Request.builder()
                    .cep(request.getCep() != null ? request.getCep() : entity.getCep())
                    .number(request.getNumber() != null ? request.getNumber() : entity.getNumber())
                    .complement(request.getComplement() != null ? request.getComplement() : entity.getComplement())
                    .street(request.getStreet() != null ? request.getStreet() : entity.getStreet())
                    .neighborhood(request.getNeighborhood() != null ? request.getNeighborhood() : entity.getNeighborhood())
                    .city(request.getCity() != null ? request.getCity() : entity.getCity())
                    .state(request.getState() != null ? request.getState() : entity.getState())
                    .build();
            entity = solicitationService.saveStep2(id, clientId, step2);
        }

        if (request.getPriority() != null || request.getPreferredDate() != null) {
            SolicitationStep3Request step3 = SolicitationStep3Request.builder()
                    .priority(request.getPriority() != null ? request.getPriority() : entity.getPriority())
                    .preferredDate(request.getPreferredDate() != null ? request.getPreferredDate() : entity.getPreferredDate())
                    .estimatedValue(request.getEstimatedValue() != null ? request.getEstimatedValue() : entity.getEstimatedValue())
                    .termsAccepted(request.getTermsAccepted() != null ? request.getTermsAccepted() : entity.getTermsAccepted())
                    .build();
            entity = solicitationService.saveStep3(id, clientId, step3);
        }

        if (Boolean.TRUE.equals(request.getSubmitDirectly()) && entity.getStatus() == SolicitationStatus.DRAFT) {
            entity = solicitationService.submit(id, clientId);
        }

        log.info("ADMIN updated solicitation: {}", entity.getId());
        return ResponseEntity.ok(toResponse(entity));
    }

    @DeleteMapping("/solicitations/{id}")
    @Operation(summary = "Delete any solicitation (ADMIN only)")
    public ResponseEntity<Void> deleteSolicitation(@PathVariable UUID id) {
        log.info("=== ADMIN DELETING SOLICITATION ===");
        log.info("ID: {}", id);

        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        solicitationRepository.delete(entity);
        log.info("Solicitation deleted: {}", id);

        return ResponseEntity.noContent().build();
    }

    // ============================================
    // SUBMIT - ADMIN
    // ============================================

    @PostMapping("/solicitations/{id}/submit")
    @Operation(summary = "Submit a solicitation as ADMIN")
    public ResponseEntity<SolicitationResponse> submitSolicitationAsAdmin(@PathVariable UUID id) {
        log.info("=== ADMIN SUBMITTING SOLICITATION ===");
        log.info("Solicitation ID: {}", id);

        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        if (entity.getStatus() != SolicitationStatus.DRAFT) {
            throw new BusinessException("Solicitation must be in DRAFT status to submit");
        }

        // Validar se a solicitação está completa
        validateCompleteSolicitation(entity);

        entity.setStatus(SolicitationStatus.SUBMITTED);
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setAnalyzedBy(getCurrentUserId());

        SolicitationEntity saved = solicitationRepository.save(entity);
        log.info("Solicitation submitted by ADMIN: {}", saved.getId());

        return ResponseEntity.ok(toResponse(saved));
    }

    // ============================================
    // START ANALYSIS - ADMIN
    // ============================================

    @PostMapping("/solicitations/{id}/start-analysis")
    @Operation(summary = "Start analysis of a solicitation (ADMIN)")
    public ResponseEntity<SolicitationResponse> startAnalysis(@PathVariable UUID id) {
        log.info("=== ADMIN STARTING ANALYSIS ===");
        log.info("Solicitation ID: {}", id);

        UUID adminId = getCurrentUserId();

        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        if (entity.getStatus() != SolicitationStatus.SUBMITTED) {
            throw new BusinessException("Solicitation must be in SUBMITTED status to start analysis");
        }

        entity.setStatus(SolicitationStatus.IN_REVIEW);
        entity.setAnalyzedAt(LocalDateTime.now());
        entity.setAnalyzedBy(adminId);

        SolicitationEntity saved = solicitationRepository.save(entity);
        log.info("Analysis started by ADMIN: {}", saved.getId());

        return ResponseEntity.ok(toResponse(saved));
    }

    // ============================================
    // DECIDE - ADMIN
    // ============================================

    @PostMapping("/solicitations/{id}/decide")
    @Operation(summary = "Decide solicitation (APPROVE or REJECT) - ADMIN")
    public ResponseEntity<SolicitationResponse> decideSolicitation(
            @PathVariable UUID id,
            @Valid @RequestBody AnalystDecisionRequest request) {

        log.info("=== ADMIN DECIDING SOLICITATION ===");
        log.info("Solicitation ID: {}", id);
        log.info("Decision: {}", request.getDecision());

        UUID adminId = getCurrentUserId();

        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        if (entity.getStatus() != SolicitationStatus.SUBMITTED &&
                entity.getStatus() != SolicitationStatus.IN_REVIEW) {
            throw new BusinessException("Solicitation must be in SUBMITTED or IN_REVIEW status to be decided");
        }

        String decision = request.getDecision().toUpperCase();
        if (!decision.equals("APPROVE") && !decision.equals("REJECT")) {
            throw new BusinessException("Invalid decision. Use APPROVE or REJECT");
        }

        if (decision.equals("APPROVE")) {
            entity.setStatus(SolicitationStatus.APPROVED);
            log.info("Solicitation APPROVED by ADMIN");
        } else {
            entity.setStatus(SolicitationStatus.REJECTED);
            log.info("Solicitation REJECTED by ADMIN");
        }

        entity.setAnalysisComment(request.getComment());
        entity.setAnalyzedAt(LocalDateTime.now());
        entity.setAnalyzedBy(adminId);

        SolicitationEntity saved = solicitationRepository.save(entity);
        log.info("Solicitation decided by ADMIN: {}", saved.getId());

        return ResponseEntity.ok(toResponse(saved));
    }

    // ============================================
    // REQUEST REVISION - ADMIN
    // ============================================

    @PostMapping("/solicitations/{id}/request-revision")
    @Operation(summary = "Request revision from client (ADMIN)")
    public ResponseEntity<SolicitationResponse> requestRevision(
            @PathVariable UUID id,
            @RequestBody @Valid AdminRevisionRequest request) {

        log.info("=== ADMIN REQUESTING REVISION ===");
        log.info("Solicitation ID: {}", id);

        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        if (entity.getStatus() != SolicitationStatus.IN_REVIEW) {
            throw new BusinessException("Solicitation must be IN_REVIEW to request revision");
        }

        entity.setStatus(SolicitationStatus.DRAFT);
        entity.setAnalysisComment(request.getComment());
        entity.setAnalyzedAt(LocalDateTime.now());
        entity.setAnalyzedBy(getCurrentUserId());

        SolicitationEntity saved = solicitationRepository.save(entity);
        log.info("Revision requested by ADMIN: {}", saved.getId());

        return ResponseEntity.ok(toResponse(saved));
    }

    // ============================================
    // STATUS UPDATE - ADMIN
    // ============================================

    @PutMapping("/solicitations/{id}/status")
    @Operation(summary = "Update solicitation status (ADMIN)")
    public ResponseEntity<SolicitationResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody StatusUpdateRequest request) {

        log.info("=== ADMIN UPDATING STATUS ===");
        log.info("Solicitation ID: {}", id);
        log.info("New Status: {}", request.getStatus());

        SolicitationEntity entity = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        try {
            SolicitationStatus newStatus = SolicitationStatus.valueOf(request.getStatus().toUpperCase());

            validateStatusTransition(entity.getStatus(), newStatus);

            entity.setStatus(newStatus);
            entity.setAnalyzedAt(LocalDateTime.now());
            entity.setAnalyzedBy(getCurrentUserId());

            SolicitationEntity saved = solicitationRepository.save(entity);
            log.info("Status updated: {} -> {}", entity.getStatus(), saved.getStatus());

            return ResponseEntity.ok(toResponse(saved));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status: " + request.getStatus());
        }
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
        stats.put("draftSolicitations", solicitationRepository.countByStatus(SolicitationStatus.DRAFT));
        return ResponseEntity.ok(stats);
    }

    // ============================================
    // MÉTODOS AUXILIARES
    // ============================================

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Valida se a solicitação está completa para ser enviada
     */
    private void validateCompleteSolicitation(SolicitationEntity entity) {
        // Step 1
        if (entity.getServiceType() == null) {
            throw new BusinessException("Step 1 incomplete: Service type is required");
        }
        if (entity.getTitle() == null || entity.getTitle().length() < 3) {
            throw new BusinessException("Step 1 incomplete: Title must be between 3 and 80 characters");
        }
        if (entity.getDescription() == null || entity.getDescription().length() < 20) {
            throw new BusinessException("Step 1 incomplete: Description must be between 20 and 1000 characters");
        }

        // Step 2
        if (entity.getCep() == null || entity.getCep().isEmpty()) {
            throw new BusinessException("Step 2 incomplete: CEP is required");
        }
        if (entity.getNumber() == null || entity.getNumber().isEmpty()) {
            throw new BusinessException("Step 2 incomplete: Number is required");
        }
        if (entity.getStreet() == null || entity.getStreet().isEmpty()) {
            throw new BusinessException("Step 2 incomplete: Street is required");
        }
        if (entity.getNeighborhood() == null || entity.getNeighborhood().isEmpty()) {
            throw new BusinessException("Step 2 incomplete: Neighborhood is required");
        }
        if (entity.getCity() == null || entity.getCity().isEmpty()) {
            throw new BusinessException("Step 2 incomplete: City is required");
        }
        if (entity.getState() == null || entity.getState().isEmpty()) {
            throw new BusinessException("Step 2 incomplete: State is required");
        }

        // Step 3
        if (entity.getPriority() == null) {
            throw new BusinessException("Step 3 incomplete: Priority is required");
        }
        if (entity.getPreferredDate() == null) {
            throw new BusinessException("Step 3 incomplete: Preferred date is required");
        }
        if (entity.getEstimatedValue() == null || entity.getEstimatedValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Step 3 incomplete: Estimated value is required");
        }
        if (!Boolean.TRUE.equals(entity.getTermsAccepted())) {
            throw new BusinessException("Step 3 incomplete: Terms must be accepted");
        }

        // Regra adicional: HIGH priority requer valor >= 100
        if (entity.getPriority() == Priority.HIGH &&
                entity.getEstimatedValue().compareTo(new BigDecimal("100")) < 0) {
            throw new BusinessException("For HIGH priority, estimated value must be >= 100");
        }
    }

    /**
     * Valida transição de status
     */
    private void validateStatusTransition(SolicitationStatus current, SolicitationStatus newStatus) {
        if (current == SolicitationStatus.DRAFT) {
            if (newStatus != SolicitationStatus.SUBMITTED && newStatus != SolicitationStatus.DRAFT) {
                throw new BusinessException("From DRAFT, only SUBMITTED is allowed");
            }
        } else if (current == SolicitationStatus.SUBMITTED) {
            if (newStatus != SolicitationStatus.IN_REVIEW) {
                throw new BusinessException("From SUBMITTED, only IN_REVIEW is allowed");
            }
        } else if (current == SolicitationStatus.IN_REVIEW) {
            if (newStatus != SolicitationStatus.APPROVED &&
                    newStatus != SolicitationStatus.REJECTED &&
                    newStatus != SolicitationStatus.DRAFT) {
                throw new BusinessException("From IN_REVIEW, only APPROVED, REJECTED or DRAFT is allowed");
            }
        } else {
            throw new BusinessException("Cannot change status from " + current);
        }
    }

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

    private SolicitationResponse toResponse(SolicitationEntity entity) {
        String clientName = userRepository.findById(entity.getClientId())
                .map(UserEntity::getName)
                .orElse("Usuário não encontrado");

        return SolicitationResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .clientName(clientName)
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