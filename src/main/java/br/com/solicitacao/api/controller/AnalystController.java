package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.annotation.Audit;
import br.com.solicitacao.api.dto.request.AnalystDecisionRequest;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.api.dto.response.SolicitationResponse;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.service.AnalystService;
import br.com.solicitacao.core.service.SolicitationService;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/analyst/solicitations")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "Endpoints for analysts")
@SecurityRequirement(name = "bearerAuth")
public class AnalystController {

    private final AnalystService analystService;
    private final SolicitationService solicitationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List solicitations for analysis (filtered by state)")
    public ResponseEntity<Page<SolicitationListResponse>> listSolicitations(
            @RequestParam(required = false) SolicitationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID analystId = getCurrentUserId();
        log.info("=== LISTANDO SOLICITAÇÕES ===");
        log.info("Analyst ID: {}", analystId);

        // Verificar se o analista tem cobertura
        List<String> states = analystService.getAnalystStates(analystId);
        log.info("Estados cobertos: {}", states);

        if (states.isEmpty()) {
            log.warn("Analista não possui cobertura! ID: {}", analystId);
            // Não lançar exceção, retornar lista vazia
            return ResponseEntity.ok(Page.empty(pageable));
        }

        Page<SolicitationListResponse> solicitations = analystService.listSolicitationsForAnalyst(analystId, status, pageable);
        return ResponseEntity.ok(solicitations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get solicitation by ID (with state validation)")
    public ResponseEntity<SolicitationResponse> getSolicitation(@PathVariable UUID id) {
        UUID analystId = getCurrentUserId();
        SolicitationEntity entity = analystService.getSolicitationForAnalyst(id, analystId);
        return ResponseEntity.ok(toResponse(entity));
    }

    @PostMapping("/{id}/start")
    @Audit(action = "START_ANALYSIS", entity = "SOLICITATION")
    @Operation(summary = "Start analysis (SUBMITTED → IN_REVIEW)")
    public ResponseEntity<SolicitationResponse> startAnalysis(@PathVariable UUID id) {
        log.info("=== START ANALYSIS ===");
        log.info("Solicitation ID: {}", id);

        UUID analystId = getCurrentUserId();
        log.info("Analyst ID: {}", analystId);

        SolicitationEntity entity = analystService.startAnalysis(id, analystId);
        log.info("Analysis started successfully. Status: {}", entity.getStatus());

        return ResponseEntity.ok(toResponse(entity));
    }

    @PostMapping("/{id}/decide")
    @Audit(action = "DECIDE", entity = "SOLICITATION")
    @Operation(summary = "Decide solicitation (APPROVE or REJECT)")
    public ResponseEntity<SolicitationResponse> decide(
            @PathVariable UUID id,
            @Valid @RequestBody AnalystDecisionRequest request) {
        log.info("=== DECIDE SOLICITATION ===");
        log.info("Solicitation ID: {}", id);
        log.info("Decision: {}", request.getDecision());
        log.info("Comment: {}", request.getComment());

        UUID analystId = getCurrentUserId();
        log.info("Analyst ID: {}", analystId);

        SolicitationEntity entity = analystService.decide(id, analystId, request);
        log.info("Solicitation decided. Status: {}", entity.getStatus());

        return ResponseEntity.ok(toResponse(entity));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("Authenticated user: {}", email);

        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
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