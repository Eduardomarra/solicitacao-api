package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.SolicitationStep1Request;
import br.com.solicitacao.api.dto.request.SolicitationStep2Request;
import br.com.solicitacao.api.dto.request.SolicitationStep3Request;
import br.com.solicitacao.api.dto.response.SolicitationResponse;
import br.com.solicitacao.core.service.SolicitationService;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/solicitations")
@RequiredArgsConstructor
@Tag(name = "Solicitações", description = "Endpoints para gerenciamento de solicitações")
@SecurityRequirement(name = "bearerAuth")
public class SolicitationController {

    private final SolicitationService solicitationService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Criar nova solicitação (rascunho)")
    @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso")
    public ResponseEntity<SolicitationResponse> create() {
        UUID clientId = getCurrentUserId();
        SolicitationEntity entity = solicitationService.create(clientId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(entity));
    }

    @PutMapping("/{id}/step1")
    @Operation(summary = "Salvar Step 1 - Dados básicos")
    @ApiResponse(responseCode = "200", description = "Step 1 salvo com sucesso")
    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    public ResponseEntity<SolicitationResponse> saveStep1(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitationStep1Request request) {
        UUID clientId = getCurrentUserId();
        SolicitationEntity entity = solicitationService.saveStep1(id, clientId, request);
        return ResponseEntity.ok(toResponse(entity));
    }

    @PutMapping("/{id}/step2")
    @Operation(summary = "Salvar Step 2 - Endereço (com integração ViaCEP)")
    @ApiResponse(responseCode = "200", description = "Step 2 salvo com sucesso")
    @ApiResponse(responseCode = "400", description = "CEP inválido ou não encontrado")
    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    public ResponseEntity<SolicitationResponse> saveStep2(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitationStep2Request request) {
        UUID clientId = getCurrentUserId();
        SolicitationEntity entity = solicitationService.saveStep2(id, clientId, request);
        return ResponseEntity.ok(toResponse(entity));
    }

    @PutMapping("/{id}/step3")
    @Operation(summary = "Salvar Step 3 - Confirmação")
    @ApiResponse(responseCode = "200", description = "Step 3 salvo com sucesso")
    @ApiResponse(responseCode = "400", description = "Erro de validação (ex: HIGH priority com valor < 100)")
    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    public ResponseEntity<SolicitationResponse> saveStep3(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitationStep3Request request) {
        UUID clientId = getCurrentUserId();
        SolicitationEntity entity = solicitationService.saveStep3(id, clientId, request);
        return ResponseEntity.ok(toResponse(entity));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Enviar solicitação para análise")
    @ApiResponse(responseCode = "200", description = "Solicitação enviada com sucesso")
    @ApiResponse(responseCode = "400", description = "Solicitação incompleta")
    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    public ResponseEntity<SolicitationResponse> submit(@PathVariable UUID id) {
        UUID clientId = getCurrentUserId();
        SolicitationEntity entity = solicitationService.submit(id, clientId);
        return ResponseEntity.ok(toResponse(entity));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar solicitação por ID")
    @ApiResponse(responseCode = "200", description = "Solicitação encontrada")
    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada")
    public ResponseEntity<SolicitationResponse> findById(@PathVariable UUID id) {
        UUID clientId = getCurrentUserId();
        SolicitationEntity entity = solicitationService.findByIdAndClientId(id, clientId);
        return ResponseEntity.ok(toResponse(entity));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
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