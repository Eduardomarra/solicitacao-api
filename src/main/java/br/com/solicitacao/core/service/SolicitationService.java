package br.com.solicitacao.core.service;

import br.com.solicitacao.api.dto.request.SolicitationStep1Request;
import br.com.solicitacao.api.dto.request.SolicitationStep2Request;
import br.com.solicitacao.api.dto.request.SolicitationStep3Request;
import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.core.exception.ResourceNotFoundException;
import br.com.solicitacao.infrastructure.client.ViaCepClient;
import br.com.solicitacao.infrastructure.client.ViaCepResponse;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final ViaCepClient viaCepClient;
    private final ElasticsearchService elasticsearchService;

    @Transactional
    public SolicitationEntity create(UUID clientId) {
        log.info("=== CREATE SOLICITATION ===");
        log.info("ClientId: {}", clientId);

        SolicitationEntity solicitation = SolicitationEntity.builder()
                .clientId(clientId)
                .status(SolicitationStatus.DRAFT)
                .currentStep(0)
                .build();

        SolicitationEntity saved = solicitationRepository.save(solicitation);
        elasticsearchService.indexSolicitation(saved.getId());

        log.info("Solicitation created with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public SolicitationEntity saveStep1(UUID id, UUID clientId, SolicitationStep1Request request) {
        log.info("=== SAVE STEP 1 ===");
        log.info("Solicitation ID: {}", id);
        log.info("ClientId: {}", clientId);
        log.info("ServiceType: {}", request.getServiceType());
        log.info("Title: {}", request.getTitle());
        log.info("Description length: {}", request.getDescription().length());

        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            log.warn("Solicitation not in DRAFT status: {}", solicitation.getStatus());
            throw new BusinessException("Não é possível editar uma solicitação que já foi enviada");
        }

        solicitation.setServiceType(request.getServiceType());
        solicitation.setTitle(request.getTitle());
        solicitation.setDescription(request.getDescription());

        if (solicitation.getCurrentStep() < 1) {
            solicitation.setCurrentStep(1);
        }

        SolicitationEntity saved = solicitationRepository.save(solicitation);
        elasticsearchService.indexSolicitation(saved.getId());
        log.info("Step 1 saved successfully. Current step: {}", saved.getCurrentStep());
        return saved;
    }

    @Transactional
    public SolicitationEntity saveStep2(UUID id, UUID clientId, SolicitationStep2Request request) {
        log.info("=== SAVE STEP 2 ===");
        log.info("Solicitation ID: {}", id);
        log.info("CEP: {}", request.getCep());
        log.info("Number: {}", request.getNumber());

        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new BusinessException("Não é possível editar uma solicitação que já foi enviada");
        }

        // Limpar CEP
        String cepLimpo = request.getCep().replace("-", "");
        log.info("Cleaned CEP: {}", cepLimpo);

        // Buscar endereço no ViaCEP
        try {
            ViaCepResponse viaCepResponse = viaCepClient.buscarCep(cepLimpo);
            log.info("ViaCEP response: {}", viaCepResponse);

            if (viaCepResponse.isErro() || viaCepResponse.getCep() == null) {
                log.error("Invalid CEP: {}", cepLimpo);
                throw new BusinessException("CEP inválido ou não encontrado");
            }

            solicitation.setCep(cepLimpo);
            solicitation.setNumber(request.getNumber());
            solicitation.setComplement(request.getComplement());

            // Preencher com dados do ViaCEP
            solicitation.setStreet(viaCepResponse.getLogradouro());
            solicitation.setNeighborhood(viaCepResponse.getBairro());
            solicitation.setCity(viaCepResponse.getLocalidade());
            solicitation.setState(viaCepResponse.getUf());

            // Sobrescrever se usuário enviou dados diferentes
            if (request.getStreet() != null && !request.getStreet().isEmpty()) {
                solicitation.setStreet(request.getStreet());
            }
            if (request.getNeighborhood() != null && !request.getNeighborhood().isEmpty()) {
                solicitation.setNeighborhood(request.getNeighborhood());
            }
            if (request.getCity() != null && !request.getCity().isEmpty()) {
                solicitation.setCity(request.getCity());
            }
            if (request.getState() != null && !request.getState().isEmpty()) {
                solicitation.setState(request.getState());
            }

        } catch (Exception e) {
            log.error("Error fetching CEP: {}", e.getMessage(), e);
            throw new BusinessException("Erro ao consultar CEP: " + e.getMessage());
        }

        if (isStep2Complete(solicitation)) {
            if (solicitation.getCurrentStep() < 2) {
                solicitation.setCurrentStep(2);
            }
        }

        SolicitationEntity saved = solicitationRepository.save(solicitation);
        elasticsearchService.indexSolicitation(saved.getId());
        log.info("Step 2 saved. Current step: {}, State: {}", saved.getCurrentStep(), saved.getState());
        return saved;
    }

    @Transactional
    public SolicitationEntity saveStep3(UUID id, UUID clientId, SolicitationStep3Request request) {
        log.info("=== SAVE STEP 3 ===");
        log.info("Solicitation ID: {}", id);
        log.info("Priority: {}", request.getPriority());
        log.info("Estimated Value: {}", request.getEstimatedValue());
        log.info("Terms Accepted: {}", request.getTermsAccepted());

        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new BusinessException("Não é possível editar uma solicitação que já foi enviada");
        }

        // Validar regra: HIGH priority requer valor >= 100
        if (request.getPriority() == Priority.HIGH &&
                request.getEstimatedValue().compareTo(new BigDecimal("100")) < 0) {
            log.warn("HIGH priority with value < 100: {}", request.getEstimatedValue());
            throw new BusinessException("Para prioridade HIGH, o valor estimado deve ser >= 100");
        }

        solicitation.setPriority(request.getPriority());
        solicitation.setPreferredDate(request.getPreferredDate());
        solicitation.setEstimatedValue(request.getEstimatedValue());
        solicitation.setTermsAccepted(request.getTermsAccepted());

        if (isStep3Complete(solicitation)) {
            solicitation.setCurrentStep(3);
        }

        SolicitationEntity saved = solicitationRepository.save(solicitation);
        elasticsearchService.indexSolicitation(saved.getId());
        log.info("Step 3 saved. Current step: {}", saved.getCurrentStep());
        return saved;
    }

    @Transactional
    public SolicitationEntity submit(UUID id, UUID clientId) {
        log.info("=== SUBMIT SOLICITATION ===");
        log.info("ID: {}", id);
        log.info("ClientId: {}", clientId);

        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);
        log.info("Solicitation found: status={}, currentStep={}", solicitation.getStatus(), solicitation.getCurrentStep());

        log.info("Step 1: serviceType={}, title={}", solicitation.getServiceType(), solicitation.getTitle());
        log.info("Step 2: cep={}, number={}, street={}, neighborhood={}, city={}, state={}",
                solicitation.getCep(), solicitation.getNumber(), solicitation.getStreet(),
                solicitation.getNeighborhood(), solicitation.getCity(), solicitation.getState());
        log.info("Step 3: priority={}, preferredDate={}, estimatedValue={}, termsAccepted={}",
                solicitation.getPriority(), solicitation.getPreferredDate(),
                solicitation.getEstimatedValue(), solicitation.getTermsAccepted());

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            log.warn("Solicitation not in DRAFT status: {}", solicitation.getStatus());
            throw new BusinessException("Solicitação já foi enviada ou está em análise");
        }

        try {
            validateCompleteSolicitation(solicitation);
            log.info("Validation passed successfully");
        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage(), e);
            throw e;
        }

        solicitation.setStatus(SolicitationStatus.SUBMITTED);
        solicitation.setSubmittedAt(LocalDateTime.now());

        SolicitationEntity saved = solicitationRepository.save(solicitation);
        elasticsearchService.indexSolicitation(saved.getId());
        log.info("Solicitation submitted successfully: id={}, status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    public SolicitationEntity findByIdAndClientId(UUID id, UUID clientId) {
        log.info("Finding solicitation: id={}, clientId={}", id, clientId);
        return solicitationRepository.findById(id)
                .filter(s -> s.getClientId().equals(clientId))
                .orElseThrow(() -> {
                    log.error("Solicitation not found: id={}, clientId={}", id, clientId);
                    return new ResourceNotFoundException("Solicitação não encontrada");
                });
    }

    private boolean isStep2Complete(SolicitationEntity solicitation) {
        return solicitation.getCep() != null && !solicitation.getCep().isEmpty()
                && solicitation.getNumber() != null && !solicitation.getNumber().isEmpty()
                && solicitation.getStreet() != null && !solicitation.getStreet().isEmpty()
                && solicitation.getNeighborhood() != null && !solicitation.getNeighborhood().isEmpty()
                && solicitation.getCity() != null && !solicitation.getCity().isEmpty()
                && solicitation.getState() != null && !solicitation.getState().isEmpty();
    }

    private boolean isStep3Complete(SolicitationEntity solicitation) {
        return solicitation.getPriority() != null
                && solicitation.getPreferredDate() != null
                && solicitation.getEstimatedValue() != null
                && Boolean.TRUE.equals(solicitation.getTermsAccepted());
    }

    private void validateCompleteSolicitation(SolicitationEntity solicitation) {
        log.info("=== VALIDATING COMPLETE SOLICITATION ===");

        // Step 1
        if (solicitation.getServiceType() == null) {
            log.error("Step 1 incomplete: serviceType is null");
            throw new BusinessException("Step 1 incompleto: Tipo de serviço obrigatório");
        }
        if (solicitation.getTitle() == null || solicitation.getTitle().length() < 3) {
            log.error("Step 1 incomplete: title is null or too short: {}", solicitation.getTitle());
            throw new BusinessException("Step 1 incompleto: Título deve ter entre 3 e 80 caracteres");
        }
        if (solicitation.getDescription() == null || solicitation.getDescription().length() < 20) {
            log.error("Step 1 incomplete: description is null or too short: length={}",
                    solicitation.getDescription() != null ? solicitation.getDescription().length() : 0);
            throw new BusinessException("Step 1 incompleto: Descrição deve ter entre 20 e 1000 caracteres");
        }

        // Step 2
        if (!isStep2Complete(solicitation)) {
            log.error("Step 2 incomplete");
            throw new BusinessException("Step 2 incompleto: Endereço não está totalmente preenchido");
        }

        // Step 3
        if (!isStep3Complete(solicitation)) {
            log.error("Step 3 incomplete");
            throw new BusinessException("Step 3 incompleto: Dados de confirmação não estão totalmente preenchidos");
        }

        // Regra adicional: HIGH priority requer valor >= 100
        if (solicitation.getPriority() == Priority.HIGH &&
                solicitation.getEstimatedValue().compareTo(new BigDecimal("100")) < 0) {
            log.error("HIGH priority requires value >= 100, but got: {}", solicitation.getEstimatedValue());
            throw new BusinessException("Para prioridade HIGH, o valor estimado deve ser >= 100");
        }

        log.info("Validation completed successfully");
    }

    public SolicitationEntity findById(UUID id) {
        log.info("Buscando solicitação por ID: {}", id);
        return solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
    }
}