package br.com.solicitacao.core.service;

import br.com.solicitacao.api.dto.request.AnalystDecisionRequest;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.core.exception.ResourceNotFoundException;
import br.com.solicitacao.infrastructure.persistence.entity.AnalystCoverageEntity;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.AnalystCoverageRepository;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalystService {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final AnalystCoverageRepository analystCoverageRepository;

    @Transactional
    public void updateCoverage(UUID userId, List<String> states) {
        log.info("Updating analyst coverage: userId={}, states={}", userId, states);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole().name().equals("ANALYST")) {
            analystCoverageRepository.deleteByUserId(userId);

            for (String state : states) {
                AnalystCoverageEntity coverage = AnalystCoverageEntity.builder()
                        .userId(userId)
                        .state(state.toUpperCase())
                        .build();
                analystCoverageRepository.save(coverage);
            }
            log.info("Coverage updated successfully: userId={}", userId);
        } else {
            throw new BusinessException("Only analysts can have state coverage");
        }
    }

    public List<String> getAnalystStates(UUID userId) {
        log.info("Buscando estados do analista: userId={}", userId);
        List<AnalystCoverageEntity> coverages = analystCoverageRepository.findByUserId(userId);
        log.info("Coberturas encontradas: {}", coverages.size());

        return coverages.stream()
                .map(AnalystCoverageEntity::getState)
                .collect(Collectors.toList());
    }

    public Page<SolicitationListResponse> listSolicitationsForAnalyst(
            UUID analystId,
            SolicitationStatus status,
            Pageable pageable) {

        log.info("=== LISTANDO SOLICITAÇÕES PARA ANALISTA ===");
        log.info("Analyst ID: {}", analystId);

        List<String> states = getAnalystStates(analystId);
        log.info("Estados cobertos: {}", states);

        if (states.isEmpty()) {
            log.warn("Analista não possui cobertura de estados! ID: {}", analystId);
            return Page.empty(pageable);  // Retorna página vazia em vez de lançar exceção
        }

        SolicitationStatus statusFilter = status != null ? status : SolicitationStatus.SUBMITTED;
        log.info("Status filtro: {}", statusFilter);

        Page<SolicitationEntity> entities = solicitationRepository
                .findByStateInAndStatus(states, statusFilter, pageable);

        log.info("Total de solicitações encontradas: {}", entities.getTotalElements());
        return entities.map(this::toListResponse);
    }

    public SolicitationEntity getSolicitationForAnalyst(UUID id, UUID analystId) {
        log.info("Getting solicitation for analyst: id={}, analystId={}", id, analystId);

        SolicitationEntity solicitation = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found"));

        List<String> analystStates = getAnalystStates(analystId);
        if (!analystStates.contains(solicitation.getState())) {
            throw new BusinessException("Analyst does not have permission to access solicitations from this state: " + solicitation.getState());
        }

        return solicitation;
    }

    @Transactional
    public SolicitationEntity startAnalysis(UUID id, UUID analystId) {
        log.info("=== START ANALYSIS SERVICE ===");
        log.info("Solicitation ID: {}", id);
        log.info("Analyst ID: {}", analystId);

        SolicitationEntity solicitation = getSolicitationForAnalyst(id, analystId);
        log.info("Solicitation found. Status: {}", solicitation.getStatus());

        if (solicitation.getStatus() != SolicitationStatus.SUBMITTED) {
            log.warn("Solicitation not in SUBMITTED status: {}", solicitation.getStatus());
            throw new BusinessException("Solicitation must be in SUBMITTED status to start analysis");
        }

        solicitation.setStatus(SolicitationStatus.IN_REVIEW);
        solicitation.setAnalyzedAt(LocalDateTime.now());
        solicitation.setAnalyzedBy(analystId);

        SolicitationEntity saved = solicitationRepository.save(solicitation);
        log.info("Analysis started successfully. Status: {}", saved.getStatus());

        return saved;
    }

    @Transactional
    public SolicitationEntity decide(UUID id, UUID analystId, AnalystDecisionRequest request) {
        log.info("=== DECIDE SERVICE ===");
        log.info("Solicitation ID: {}", id);
        log.info("Analyst ID: {}", analystId);
        log.info("Decision: {}", request.getDecision());

        SolicitationEntity solicitation = getSolicitationForAnalyst(id, analystId);
        log.info("Solicitation found. Status: {}", solicitation.getStatus());

        if (solicitation.getStatus() != SolicitationStatus.SUBMITTED &&
                solicitation.getStatus() != SolicitationStatus.IN_REVIEW) {
            log.warn("Solicitation not in SUBMITTED or IN_REVIEW status: {}", solicitation.getStatus());
            throw new BusinessException("Solicitation must be in SUBMITTED or IN_REVIEW status to be decided");
        }

        String decision = request.getDecision().toUpperCase();
        if (!decision.equals("APPROVE") && !decision.equals("REJECT")) {
            throw new BusinessException("Invalid decision. Use APPROVE or REJECT");
        }

        if (decision.equals("APPROVE")) {
            solicitation.setStatus(SolicitationStatus.APPROVED);
            log.info("Solicitation APPROVED");
        } else {
            solicitation.setStatus(SolicitationStatus.REJECTED);
            log.info("Solicitation REJECTED");
        }

        solicitation.setAnalysisComment(request.getComment());
        solicitation.setAnalyzedAt(LocalDateTime.now());
        solicitation.setAnalyzedBy(analystId);

        SolicitationEntity saved = solicitationRepository.save(solicitation);
        log.info("Solicitation decided successfully. Status: {}", saved.getStatus());

        return saved;
    }

    private SolicitationListResponse toListResponse(SolicitationEntity entity) {
        String clientName = userRepository.findById(entity.getClientId())
                .map(UserEntity::getName)
                .orElse("User not found");

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