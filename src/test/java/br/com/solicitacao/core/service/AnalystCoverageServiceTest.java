package br.com.solicitacao.core.service;

import br.com.solicitacao.core.domain.enums.Role;
import br.com.solicitacao.core.exception.BusinessException;
import br.com.solicitacao.infrastructure.persistence.entity.AnalystCoverageEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import br.com.solicitacao.infrastructure.persistence.repository.AnalystCoverageRepository;
import br.com.solicitacao.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do AnalystCoverageService")
class AnalystCoverageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnalystCoverageRepository analystCoverageRepository;

    @InjectMocks
    private AnalystService analystService;

    private UUID userId;
    private UserEntity user;
    private List<String> states;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserEntity.builder()
                .id(userId)
                .name("Analista Teste")
                .email("analista@teste.com")
                .role(Role.ANALYST)
                .enabled(true)
                .build();

        states = List.of("SP", "RJ");
    }

    @Test
    @DisplayName("Deve configurar cobertura do analista com sucesso")
    void deveConfigurarCoberturaDoAnalistaComSucesso() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(analystCoverageRepository).deleteByUserId(userId);
        when(analystCoverageRepository.save(any(AnalystCoverageEntity.class)))
                .thenReturn(new AnalystCoverageEntity());

        // Act
        analystService.updateCoverage(userId, states);

        // Assert
        verify(analystCoverageRepository).deleteByUserId(userId);
        verify(analystCoverageRepository, times(2)).save(any(AnalystCoverageEntity.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao configurar cobertura para usuário não ANALYST")
    void deveLancarExcecaoAoConfigurarCoberturaParaUsuarioNaoAnalyst() {
        // Arrange
        user.setRole(Role.CLIENT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> analystService.updateCoverage(userId, states))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only analysts can have state coverage");  // ✅ Mensagem em inglês
    }

    @Test
    @DisplayName("Deve buscar estados cobertos pelo analista")
    void deveBuscarEstadosCobertosPeloAnalista() {
        // Arrange
        List<AnalystCoverageEntity> coverages = List.of(
                AnalystCoverageEntity.builder().userId(userId).state("SP").build(),
                AnalystCoverageEntity.builder().userId(userId).state("RJ").build()
        );
        when(analystCoverageRepository.findByUserId(userId)).thenReturn(coverages);

        // Act
        List<String> result = analystService.getAnalystStates(userId);

        // Assert
        assertThat(result).containsExactly("SP", "RJ");
        verify(analystCoverageRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando analista não tem cobertura")
    void deveRetornarListaVaziaQuandoAnalistaNaoTemCobertura() {
        // Arrange
        when(analystCoverageRepository.findByUserId(userId)).thenReturn(List.of());

        // Act
        List<String> result = analystService.getAnalystStates(userId);

        // Assert
        assertThat(result).isEmpty();
        verify(analystCoverageRepository).findByUserId(userId);
    }
}