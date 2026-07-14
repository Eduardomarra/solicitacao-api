package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.AnalystDecisionRequest;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.api.dto.response.SolicitationResponse;
import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.core.service.AnalystService;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do AnalystController")
class AnalystControllerTest {

    @Mock
    private AnalystService analystService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AnalystController analystController;

    private UUID analystId;
    private UUID solicitationId;
    private UUID clientId;
    private String email;
    private UserEntity user;
    private SolicitationEntity solicitation;

    @BeforeEach
    void setUp() {
        analystId = UUID.randomUUID();
        solicitationId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        email = "analista@teste.com";

        user = UserEntity.builder()
                .id(analystId)
                .name("Analista Teste")
                .email(email)
                .build();

        solicitation = SolicitationEntity.builder()
                .id(solicitationId)
                .clientId(clientId)
                .status(SolicitationStatus.SUBMITTED)
                .currentStep(3)
                .serviceType(ServiceType.INSTALLATION)
                .title("Instalação de Internet")
                .description("Solicitação de instalação de internet.")
                .city("São Paulo")
                .state("SP")
                .priority(Priority.MEDIUM)
                .createdAt(LocalDateTime.now())
                .submittedAt(LocalDateTime.now())
                .build();

        // Configurar SecurityContext mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(user));
    }

    @Test
    @DisplayName("Deve listar solicitações para analista com cobertura")
    void deveListarSolicitacoesParaAnalista() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 20);

        SolicitationListResponse response = SolicitationListResponse.builder()
                .id(solicitationId)
                .clientId(clientId)
                .clientName("Cliente Teste")
                .status(SolicitationStatus.SUBMITTED)
                .currentStep(3)
                .serviceType(ServiceType.INSTALLATION)
                .title("Instalação de Internet")
                .city("São Paulo")
                .state("SP")
                .priority(Priority.MEDIUM)
                .createdAt(LocalDateTime.now())
                .submittedAt(LocalDateTime.now())
                .build();

        Page<SolicitationListResponse> page = new PageImpl<>(List.of(response));

        when(analystService.listSolicitationsForAnalyst(eq(analystId), any(), eq(pageable)))
                .thenReturn(page);

        // Act
        ResponseEntity<Page<SolicitationListResponse>> result =
                analystController.listSolicitations(null, pageable);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotalElements()).isEqualTo(1);
        assertThat(result.getBody().getContent().get(0).getState()).isEqualTo("SP");
        verify(analystService).listSolicitationsForAnalyst(eq(analystId), any(), eq(pageable));
    }

    @Test
    @DisplayName("Deve buscar solicitação por ID com sucesso")
    void deveBuscarSolicitacaoPorId() {
        // Arrange
        when(analystService.getSolicitationForAnalyst(solicitationId, analystId))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                analystController.getSolicitation(solicitationId);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(solicitationId);
        assertThat(result.getBody().getState()).isEqualTo("SP");
        verify(analystService).getSolicitationForAnalyst(solicitationId, analystId);
    }

    @Test
    @DisplayName("Deve iniciar análise com sucesso")
    void deveIniciarAnaliseComSucesso() {
        // Arrange
        solicitation.setStatus(SolicitationStatus.IN_REVIEW);
        solicitation.setAnalyzedAt(LocalDateTime.now());
        solicitation.setAnalyzedBy(analystId);

        when(analystService.startAnalysis(solicitationId, analystId))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                analystController.startAnalysis(solicitationId);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(SolicitationStatus.IN_REVIEW);
        assertThat(result.getBody().getAnalyzedBy()).isEqualTo(analystId);
        verify(analystService).startAnalysis(solicitationId, analystId);
    }

    @Test
    @DisplayName("Deve decidir solicitação com APPROVE")
    void deveDecidirSolicitacaoComApprove() {
        // Arrange
        AnalystDecisionRequest request = AnalystDecisionRequest.builder()
                .decision("APPROVE")
                .comment("Solicitação aprovada. Atende a todos os requisitos.")
                .build();

        solicitation.setStatus(SolicitationStatus.APPROVED);
        solicitation.setAnalysisComment("Solicitação aprovada. Atende a todos os requisitos.");
        solicitation.setAnalyzedAt(LocalDateTime.now());
        solicitation.setAnalyzedBy(analystId);

        when(analystService.decide(solicitationId, analystId, request))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                analystController.decide(solicitationId, request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(result.getBody().getAnalysisComment()).isEqualTo("Solicitação aprovada. Atende a todos os requisitos.");
        verify(analystService).decide(solicitationId, analystId, request);
    }

    @Test
    @DisplayName("Deve decidir solicitação com REJECT")
    void deveDecidirSolicitacaoComReject() {
        // Arrange
        AnalystDecisionRequest request = AnalystDecisionRequest.builder()
                .decision("REJECT")
                .comment("Solicitação rejeitada. Documentação incompleta.")
                .build();

        solicitation.setStatus(SolicitationStatus.REJECTED);
        solicitation.setAnalysisComment("Solicitação rejeitada. Documentação incompleta.");
        solicitation.setAnalyzedAt(LocalDateTime.now());
        solicitation.setAnalyzedBy(analystId);

        when(analystService.decide(solicitationId, analystId, request))
                .thenReturn(solicitation);

        // Act
        ResponseEntity<SolicitationResponse> result =
                analystController.decide(solicitationId, request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(SolicitationStatus.REJECTED);
        assertThat(result.getBody().getAnalysisComment()).isEqualTo("Solicitação rejeitada. Documentação incompleta.");
        verify(analystService).decide(solicitationId, analystId, request);
    }

    @Test
    @DisplayName("Deve lançar exceção quando analista não tem cobertura")
    void deveLancarExcecaoQuandoAnalistaNaoTemCobertura() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 20);

        when(analystService.listSolicitationsForAnalyst(eq(analystId), any(), eq(pageable)))
                .thenThrow(new BusinessException("Analista não possui cobertura de estados"));

        // Act & Assert
        assertThatThrownBy(() -> analystController.listSolicitations(null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Analista não possui cobertura de estados");

        verify(analystService).listSolicitationsForAnalyst(eq(analystId), any(), eq(pageable));
    }
}