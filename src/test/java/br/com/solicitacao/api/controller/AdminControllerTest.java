package br.com.solicitacao.api.controller;

import br.com.solicitacao.api.dto.request.CoverageRequest;
import br.com.solicitacao.api.dto.request.CreateUserRequest;
import br.com.solicitacao.api.dto.response.AuthResponse;
import br.com.solicitacao.api.dto.response.SolicitationListResponse;
import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.core.service.AnalystService;
import br.com.solicitacao.core.service.AuthService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do AdminController")
class AdminControllerTest {

    @Mock
    private AnalystService analystService;

    @Mock
    private AuthService authService;

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminController adminController;

    private UUID userId;
    private UUID solicitationId;
    private CreateUserRequest createUserRequest;
    private CoverageRequest coverageRequest;
    private SolicitationEntity solicitation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        solicitationId = UUID.randomUUID();

        createUserRequest = CreateUserRequest.builder()
                .name("Novo Analista")
                .email("analista@teste.com")
                .password("123456")
                .role(Role.ANALYST)
                .build();

        coverageRequest = CoverageRequest.builder()
                .states(List.of("SP", "RJ"))
                .build();

        solicitation = SolicitationEntity.builder()
                .id(solicitationId)
                .clientId(UUID.randomUUID())
                .status(SolicitationStatus.SUBMITTED)
                .currentStep(3)
                .title("Instalação de Internet")
                .city("São Paulo")
                .state("SP")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve criar usuário com sucesso (ADMIN)")
    void deveCriarUsuarioComSucesso() {
        AuthResponse response = AuthResponse.builder()
                .accessToken("jwtToken")
                .email("analista@teste.com")
                .role("ANALYST")
                .build();

        when(authService.createUser(createUserRequest)).thenReturn(response);

        ResponseEntity<AuthResponse> result = adminController.createUser(createUserRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getEmail()).isEqualTo("analista@teste.com");
        verify(authService).createUser(createUserRequest);
    }

    @Test
    @DisplayName("Deve configurar cobertura do analista")
    void deveConfigurarCoberturaDoAnalista() {
        doNothing().when(analystService).updateCoverage(userId, coverageRequest.getStates());

        ResponseEntity<Void> result = adminController.updateAnalystCoverage(userId, coverageRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(analystService).updateCoverage(userId, coverageRequest.getStates());
    }

    @Test
    @DisplayName("Deve listar todas as solicitações (ADMIN)")
    void deveListarTodasAsSolicitacoes() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 20);
        Page<SolicitationEntity> page = new PageImpl<>(List.of(solicitation));

        // Mock necessário para o toListResponse não quebrar
        when(solicitationRepository.findAll(pageable)).thenReturn(page);
        when(userRepository.findById(any())).thenReturn(Optional.of(UserEntity.builder().name("Cliente Mock").build()));

        // Act
        ResponseEntity<Page<SolicitationListResponse>> result =
                adminController.listAllSolicitations(null, null, null, pageable);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotalElements()).isEqualTo(1);
        assertThat(result.getBody().getContent().get(0).getClientName()).isEqualTo("Cliente Mock");
        verify(solicitationRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Deve retornar estatísticas do Dashboard")
    void deveRetornarEstatisticasDashboard() {
        when(userRepository.count()).thenReturn(10L);
        when(solicitationRepository.count()).thenReturn(50L);

        ResponseEntity<Map<String, Object>> result = adminController.getDashboardStats();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("totalUsers")).isEqualTo(10L);
        assertThat(result.getBody().get("totalSolicitations")).isEqualTo(50L);
    }

    @Test
    @DisplayName("Deve deletar solicitação com sucesso")
    void deveDeletarSolicitacaoComSucesso() {
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitation));
        doNothing().when(solicitationRepository).delete(solicitation);

        ResponseEntity<Void> result = adminController.deleteSolicitation(solicitationId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(solicitationRepository).delete(solicitation);
    }
}