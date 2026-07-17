package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.SolicitationStep1Request;
import br.com.solicitacao.api.dto.request.SolicitationStep2Request;
import br.com.solicitacao.api.dto.request.SolicitationStep3Request;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.api.dto.response.SolicitationResponse;
import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.service.SolicitationService;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.SolicitationRepository;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do SolicitationController")
class SolicitationControllerTest {

    @Mock
    private SolicitationService solicitationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private SolicitationRepository solicitationRepository;

    @InjectMocks
    private SolicitationController solicitationController;

    private UUID clientId;
    private UUID solicitationId;
    private String email;
    private UserEntity user;
    private SolicitationEntity solicitation;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        solicitationId = UUID.randomUUID();
        email = "cliente@teste.com";

        user = UserEntity.builder()
                .id(clientId)
                .name("Cliente Teste")
                .email(email)
                .build();

        solicitation = SolicitationEntity.builder()
                .id(solicitationId)
                .clientId(clientId)
                .status(SolicitationStatus.DRAFT)
                .currentStep(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Configurar SecurityContext mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(user));
    }

    @Test
    @DisplayName("Deve criar solicitação com sucesso")
    void deveCriarSolicitacaoComSucesso() {
        // Arrange
        when(solicitationService.create(clientId)).thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result = solicitationController.create();

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(solicitationId);
        assertThat(result.getBody().getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        verify(solicitationService).create(clientId);
    }

    @Test
    @DisplayName("Deve salvar Step 1 com sucesso")
    void deveSalvarStep1ComSucesso() {
        // Arrange
        SolicitationStep1Request request = SolicitationStep1Request.builder()
                .serviceType(ServiceType.INSTALLATION)
                .title("Instalação de Internet")
                .description("Solicitação de instalação de internet.")
                .build();

        solicitation.setServiceType(ServiceType.INSTALLATION);
        solicitation.setTitle("Instalação de Internet");
        solicitation.setDescription("Solicitação de instalação de internet.");
        solicitation.setCurrentStep(1);

        when(solicitationService.saveStep1(solicitationId, clientId, request))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                solicitationController.saveStep1(solicitationId, request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getCurrentStep()).isEqualTo(1);
        verify(solicitationService).saveStep1(solicitationId, clientId, request);
    }

    @Test
    @DisplayName("Deve salvar Step 2 com sucesso")
    void deveSalvarStep2ComSucesso() {
        // Arrange
        SolicitationStep2Request request = SolicitationStep2Request.builder()
                .cep("01310-000")
                .number("100")
                .complement("Apto 101")
                .build();

        solicitation.setCep("01310000");
        solicitation.setNumber("100");
        solicitation.setComplement("Apto 101");
        solicitation.setStreet("Avenida Paulista");
        solicitation.setNeighborhood("Bela Vista");
        solicitation.setCity("São Paulo");
        solicitation.setState("SP");
        solicitation.setCurrentStep(2);

        when(solicitationService.saveStep2(solicitationId, clientId, request))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                solicitationController.saveStep2(solicitationId, request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getCurrentStep()).isEqualTo(2);
        verify(solicitationService).saveStep2(solicitationId, clientId, request);
    }

    @Test
    @DisplayName("Deve salvar Step 3 com sucesso")
    void deveSalvarStep3ComSucesso() {
        // Arrange
        SolicitationStep3Request request = SolicitationStep3Request.builder()
                .priority(Priority.MEDIUM)
                .preferredDate(LocalDate.now().plusDays(10))
                .estimatedValue(new BigDecimal("150.00"))
                .termsAccepted(true)
                .build();

        solicitation.setPriority(Priority.MEDIUM);
        solicitation.setPreferredDate(LocalDate.now().plusDays(10));
        solicitation.setEstimatedValue(new BigDecimal("150.00"));
        solicitation.setTermsAccepted(true);
        solicitation.setCurrentStep(3);

        when(solicitationService.saveStep3(solicitationId, clientId, request))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                solicitationController.saveStep3(solicitationId, request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getCurrentStep()).isEqualTo(3);
        verify(solicitationService).saveStep3(solicitationId, clientId, request);
    }

    @Test
    @DisplayName("Deve enviar solicitação com sucesso")
    void deveEnviarSolicitacaoComSucesso() {
        // Arrange
        solicitation.setStatus(SolicitationStatus.SUBMITTED);
        solicitation.setSubmittedAt(LocalDateTime.now());

        when(solicitationService.submit(solicitationId, clientId))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                solicitationController.submit(solicitationId);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(result.getBody().getSubmittedAt()).isNotNull();
        verify(solicitationService).submit(solicitationId, clientId);
    }

    @Test
    @DisplayName("Deve buscar solicitação por ID")
    void deveBuscarSolicitacaoPorId() {
        // Arrange
        when(solicitationService.findByIdAndClientId(solicitationId, clientId))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                solicitationController.findById(solicitationId);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(solicitationId);
        verify(solicitationService).findByIdAndClientId(solicitationId, clientId);
    }

    @Test
    @DisplayName("Deve listar solicitações do próprio cliente")
    void deveListarSolicitacoesDoProprioCliente() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<SolicitationEntity> page = new PageImpl<>(List.of(solicitation));

        when(solicitationRepository.findByClientId(clientId, pageable)).thenReturn(page);
        // O mock do security context / findByEmail já resolve o usuário no Setup

        ResponseEntity<Page<SolicitationListResponse>> result =
                solicitationController.listMySolicitations(pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotalElements()).isEqualTo(1);
        verify(solicitationRepository).findByClientId(clientId, pageable);
    }

    @Test
    @DisplayName("Deve deletar solicitação em rascunho com sucesso")
    void deveDeletarSolicitacaoRascunhoComSucesso() {
        solicitation.setStatus(SolicitationStatus.DRAFT);
        when(solicitationService.findByIdAndClientId(solicitationId, clientId)).thenReturn(solicitation);

        ResponseEntity<Void> result = solicitationController.deleteSolicitation(solicitationId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(solicitationRepository).delete(solicitation);
    }

    @Test
    @DisplayName("Deve impedir exclusão de solicitação que não seja rascunho")
    void deveImpedirExclusaoDeSolicitacaoForaDeRascunho() {
        solicitation.setStatus(SolicitationStatus.SUBMITTED);
        when(solicitationService.findByIdAndClientId(solicitationId, clientId)).thenReturn(solicitation);

        org.junit.jupiter.api.Assertions.assertThrows(
                br.com.solicitacao.core.exception.BusinessException.class,
                () -> solicitationController.deleteSolicitation(solicitationId)
        );
        verify(solicitationRepository, never()).delete(any());
    }
}