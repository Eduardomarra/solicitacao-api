package br.com.solicitacao.core.service;

import br.com.solicitacao.api.dto.request.SolicitationStep1Request;
import br.com.solicitacao.api.dto.request.SolicitationStep2Request;
import br.com.solicitacao.api.dto.request.SolicitationStep3Request;
import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.infrastructure.client.ViaCepClient;
import br.com.solicitacao.infrastructure.client.ViaCepResponse;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitationServiceTest {

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private ViaCepClient viaCepClient;

    @Mock
    private ElasticsearchService elasticsearchService;

    @InjectMocks
    private SolicitationService solicitationService;

    private UUID clientId;
    private UUID solicitationId;
    private SolicitationEntity solicitation;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        solicitationId = UUID.randomUUID();

        solicitation = SolicitationEntity.builder()
                .id(solicitationId)
                .clientId(clientId)
                .status(SolicitationStatus.DRAFT)
                .currentStep(0)
                .build();
    }

    @Test
    void shouldCreateSolicitation() {
        when(solicitationRepository.save(any(SolicitationEntity.class)))
                .thenReturn(solicitation);

        SolicitationEntity result = solicitationService.create(clientId);

        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(result.getCurrentStep()).isZero();
        verify(solicitationRepository).save(any(SolicitationEntity.class));
        verify(elasticsearchService).indexSolicitation(solicitationId);
    }

    @Test
    void shouldSaveStep1() {
        SolicitationStep1Request request = SolicitationStep1Request.builder()
                .serviceType(ServiceType.INSTALLATION)
                .title("Teste Instalação")
                .description("Descrição da solicitação de teste com mais de 20 caracteres")
                .build();

        when(solicitationRepository.findById(solicitationId))
                .thenReturn(Optional.of(solicitation));
        when(solicitationRepository.save(any(SolicitationEntity.class)))
                .thenReturn(solicitation);

        SolicitationEntity result = solicitationService.saveStep1(solicitationId, clientId, request);

        assertThat(result.getServiceType()).isEqualTo(ServiceType.INSTALLATION);
        assertThat(result.getTitle()).isEqualTo("Teste Instalação");
        assertThat(result.getCurrentStep()).isEqualTo(1);
        verify(solicitationRepository).save(any(SolicitationEntity.class));
    }

    @Test
    void shouldNotSaveStep1WhenNotDraft() {
        solicitation.setStatus(SolicitationStatus.SUBMITTED);
        SolicitationStep1Request request = SolicitationStep1Request.builder()
                .serviceType(ServiceType.INSTALLATION)
                .title("Teste")
                .description("Descrição com mais de 20 caracteres para teste")
                .build();

        when(solicitationRepository.findById(solicitationId))
                .thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> solicitationService.saveStep1(solicitationId, clientId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível editar uma solicitação que já foi enviada");
    }

    @Test
    void shouldSaveStep2WithViaCep() {
        SolicitationStep2Request request = SolicitationStep2Request.builder()
                .cep("01310-000")
                .number("100")
                .complement("Apto 101")
                .build();

        ViaCepResponse viaCepResponse = ViaCepResponse.builder()
                .cep("01310000")
                .logradouro("Avenida Paulista")
                .bairro("Bela Vista")
                .localidade("São Paulo")
                .uf("SP")
                .build();

        when(solicitationRepository.findById(solicitationId))
                .thenReturn(Optional.of(solicitation));
        when(viaCepClient.buscarCep("01310000"))
                .thenReturn(viaCepResponse);
        when(solicitationRepository.save(any(SolicitationEntity.class)))
                .thenReturn(solicitation);

        SolicitationEntity result = solicitationService.saveStep2(solicitationId, clientId, request);

        assertThat(result.getCep()).isEqualTo("01310000");
        assertThat(result.getStreet()).isEqualTo("Avenida Paulista");
        assertThat(result.getCity()).isEqualTo("São Paulo");
        assertThat(result.getState()).isEqualTo("SP");
        verify(viaCepClient).buscarCep("01310000");
    }

    @Test
    void shouldNotSaveStep2WithInvalidCep() {
        SolicitationStep2Request request = SolicitationStep2Request.builder()
                .cep("00000-000")
                .number("100")
                .build();

        ViaCepResponse viaCepResponse = ViaCepResponse.builder()
                .erro(true)
                .build();

        when(solicitationRepository.findById(solicitationId))
                .thenReturn(Optional.of(solicitation));
        when(viaCepClient.buscarCep("00000000"))
                .thenReturn(viaCepResponse);

        assertThatThrownBy(() -> solicitationService.saveStep2(solicitationId, clientId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CEP inválido ou não encontrado");
    }

    @Test
    void shouldSaveStep3WithMediumPriority() {
        SolicitationStep3Request request = SolicitationStep3Request.builder()
                .priority(Priority.MEDIUM)
                .preferredDate(LocalDate.now().plusDays(10))
                .estimatedValue(new BigDecimal("150.00"))
                .termsAccepted(true)
                .build();

        when(solicitationRepository.findById(solicitationId))
                .thenReturn(Optional.of(solicitation));
        when(solicitationRepository.save(any(SolicitationEntity.class)))
                .thenReturn(solicitation);

        SolicitationEntity result = solicitationService.saveStep3(solicitationId, clientId, request);

        assertThat(result.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(result.getEstimatedValue()).isEqualTo(new BigDecimal("150.00"));
        assertThat(result.getTermsAccepted()).isTrue();
    }

    @Test
    void shouldNotSaveStep3WithHighPriorityAndLowValue() {
        SolicitationStep3Request request = SolicitationStep3Request.builder()
                .priority(Priority.HIGH)
                .preferredDate(LocalDate.now().plusDays(10))
                .estimatedValue(new BigDecimal("50.00"))
                .termsAccepted(true)
                .build();

        when(solicitationRepository.findById(solicitationId))
                .thenReturn(Optional.of(solicitation));

        assertThatThrownBy(() -> solicitationService.saveStep3(solicitationId, clientId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Para prioridade HIGH, o valor estimado deve ser >= 100");
    }
}