package br.com.solicitacao.core.service;

import br.com.solicitacao.api.dto.request.SolicitationStep1Request;
import br.com.solicitacao.api.dto.request.SolicitationStep2Request;
import br.com.solicitacao.api.dto.request.SolicitationStep3Request;
import br.com.solicitacao.api.dto.response.SolicitationResponse;
import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.core.exception.ResourceNotFoundException;
import br.com.solicitacao.infrastructure.client.ViaCepClient;
import br.com.solicitacao.infrastructure.client.ViaCepResponse;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final ViaCepClient viaCepClient;

    @Transactional
    public SolicitationEntity create(UUID clientId) {
        SolicitationEntity solicitation = SolicitationEntity.builder()
                .clientId(clientId)
                .status(SolicitationStatus.DRAFT)
                .currentStep(0)
                .build();
        return solicitationRepository.save(solicitation);
    }

    @Transactional
    public SolicitationEntity saveStep1(UUID id, UUID clientId, SolicitationStep1Request request) {
        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new BusinessException("Não é possível editar uma solicitação que já foi enviada");
        }

        solicitation.setServiceType(request.getServiceType());
        solicitation.setTitle(request.getTitle());
        solicitation.setDescription(request.getDescription());

        if (solicitation.getCurrentStep() < 1) {
            solicitation.setCurrentStep(1);
        }

        return solicitationRepository.save(solicitation);
    }

    @Transactional
    public SolicitationEntity saveStep2(UUID id, UUID clientId, SolicitationStep2Request request) {
        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new BusinessException("Não é possível editar uma solicitação que já foi enviada");
        }

        // Limpar CEP (remover máscara)
        String cepLimpo = request.getCep().replace("-", "");

        // Buscar endereço no ViaCEP
        ViaCepResponse viaCepResponse = viaCepClient.buscarCep(cepLimpo);

        if (viaCepResponse.isErro() || viaCepResponse.getCep() == null) {
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

        // Se o usuário enviou dados diferentes, sobrescrever
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

        // Validar Step 2 completo
        if (isStep2Complete(solicitation)) {
            if (solicitation.getCurrentStep() < 2) {
                solicitation.setCurrentStep(2);
            }
        }

        return solicitationRepository.save(solicitation);
    }

    @Transactional
    public SolicitationEntity saveStep3(UUID id, UUID clientId, SolicitationStep3Request request) {
        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new BusinessException("Não é possível editar uma solicitação que já foi enviada");
        }

        // Validar regra: HIGH priority requer valor >= 100
        if (request.getPriority() == Priority.HIGH &&
                request.getEstimatedValue().compareTo(new BigDecimal("100")) < 0) {
            throw new BusinessException("Para prioridade HIGH, o valor estimado deve ser >= 100");
        }

        solicitation.setPriority(request.getPriority());
        solicitation.setPreferredDate(request.getPreferredDate());
        solicitation.setEstimatedValue(request.getEstimatedValue());
        solicitation.setTermsAccepted(request.getTermsAccepted());

        if (isStep3Complete(solicitation)) {
            solicitation.setCurrentStep(3);
        }

        return solicitationRepository.save(solicitation);
    }

    @Transactional
    public SolicitationEntity submit(UUID id, UUID clientId) {
        SolicitationEntity solicitation = findByIdAndClientId(id, clientId);

        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new BusinessException("Solicitação já foi enviada ou está em análise");
        }

        // Validar todos os steps
        validateCompleteSolicitation(solicitation);

        solicitation.setStatus(SolicitationStatus.SUBMITTED);
        solicitation.setSubmittedAt(LocalDateTime.now());

        return solicitationRepository.save(solicitation);
    }

    public SolicitationEntity findByIdAndClientId(UUID id, UUID clientId) {
        return solicitationRepository.findById(id)
                .filter(s -> s.getClientId().equals(clientId))
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
    }

    public SolicitationEntity findById(UUID id) {
        return solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
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
        // Step 1
        if (solicitation.getServiceType() == null) {
            throw new BusinessException("Step 1 incompleto: Tipo de serviço obrigatório");
        }
        if (solicitation.getTitle() == null || solicitation.getTitle().length() < 3) {
            throw new BusinessException("Step 1 incompleto: Título deve ter entre 3 e 80 caracteres");
        }
        if (solicitation.getDescription() == null || solicitation.getDescription().length() < 20) {
            throw new BusinessException("Step 1 incompleto: Descrição deve ter entre 20 e 1000 caracteres");
        }

        // Step 2
        if (!isStep2Complete(solicitation)) {
            throw new BusinessException("Step 2 incompleto: Endereço não está totalmente preenchido");
        }

        // Step 3
        if (!isStep3Complete(solicitation)) {
            throw new BusinessException("Step 3 incompleto: Dados de confirmação não estão totalmente preenchidos");
        }

        // Regra adicional: HIGH priority requer valor >= 100
        if (solicitation.getPriority() == Priority.HIGH &&
                solicitation.getEstimatedValue().compareTo(new BigDecimal("100")) < 0) {
            throw new BusinessException("Para prioridade HIGH, o valor estimado deve ser >= 100");
        }
    }
}
