package br.com.solicitacao.core.service;

import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.infrastructure.elasticsearch.document.SolicitationDocument;
import br.com.solicitacao.infrastructure.elasticsearch.mapper.SolicitationDocumentMapper;
import br.com.solicitacao.infrastructure.elasticsearch.repository.SolicitationElasticsearchRepository;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do ElasticsearchService")
class ElasticsearchServiceTest {

    @Mock
    private SolicitationElasticsearchRepository elasticsearchRepository;

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SolicitationDocumentMapper documentMapper;

    @InjectMocks
    private ElasticsearchService elasticsearchService;

    private UUID solicitationId;
    private UUID clientId;
    private SolicitationEntity solicitationEntity;
    private SolicitationDocument solicitationDocument;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        solicitationId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);

        solicitationEntity = SolicitationEntity.builder()
                .id(solicitationId)
                .clientId(clientId)
                .build();

        solicitationDocument = new SolicitationDocument();
        solicitationDocument.setId(solicitationId.toString());
    }

    // ========== TESTES DE INDEXAÇÃO ==========

    @Test
    @DisplayName("Deve indexar solicitação com sucesso")
    void deveIndexarSolicitacaoComSucesso() {
        // Arrange
        UserEntity user = UserEntity.builder().name("Cliente Teste").build();

        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.of(solicitationEntity));
        when(userRepository.findById(clientId)).thenReturn(Optional.of(user));
        when(documentMapper.toDocument(solicitationEntity, "Cliente Teste")).thenReturn(solicitationDocument);

        // Act
        elasticsearchService.indexSolicitation(solicitationId);

        // Assert
        verify(elasticsearchRepository).save(solicitationDocument);
    }

    @Test
    @DisplayName("Não deve fazer nada se a solicitação não for encontrada ao indexar")
    void naoDeveFazerNadaSeSolicitacaoNaoEncontradaAoIndexar() {
        // Arrange
        when(solicitationRepository.findById(solicitationId)).thenReturn(Optional.empty());

        // Act
        elasticsearchService.indexSolicitation(solicitationId);

        // Assert
        verify(elasticsearchRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    @DisplayName("Deve engolir a exceção e não quebrar caso dê erro ao indexar (try/catch)")
    void deveEngolirExcecaoAoIndexar() {
        // Arrange
        when(solicitationRepository.findById(solicitationId))
                .thenThrow(new RuntimeException("Elasticsearch falhou"));

        // Act
        elasticsearchService.indexSolicitation(solicitationId); // Não deve lançar exceção

        // Assert
        verify(elasticsearchRepository, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("Deve excluir solicitação do Elasticsearch com sucesso")
    void deveExcluirSolicitacaoComSucesso() {
        // Act
        elasticsearchService.deleteSolicitation(solicitationId);

        // Assert
        verify(elasticsearchRepository).deleteById(solicitationId.toString());
    }

    @Test
    @DisplayName("Deve engolir a exceção e não quebrar caso dê erro ao deletar (try/catch)")
    void deveEngolirExcecaoAoDeletar() {
        // Arrange
        doThrow(new RuntimeException("Elasticsearch indisponível"))
                .when(elasticsearchRepository).deleteById(solicitationId.toString());

        // Act
        elasticsearchService.deleteSolicitation(solicitationId); // Não deve lançar exceção

        // Assert
        verify(elasticsearchRepository).deleteById(solicitationId.toString());
    }

    // ========== TESTES DE BUSCA ==========

    @Test
    @DisplayName("Deve buscar utilizando texto livre (searchWithFilters)")
    void deveBuscarComTextoLivre() {
        // Arrange
        List<SolicitationStatus> statuses = List.of(SolicitationStatus.SUBMITTED);
        List<String> states = List.of("SP");
        Page<SolicitationDocument> expectedPage = new PageImpl<>(List.of(solicitationDocument));

        when(elasticsearchRepository.searchWithFilters("Internet", statuses, states, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<SolicitationDocument> result = elasticsearchService.searchSolicitations(
                "Internet", statuses, states, null, null, null, null, pageable
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(elasticsearchRepository).searchWithFilters("Internet", statuses, states, pageable);
    }

    @Test
    @DisplayName("Deve buscar utilizando apenas os filtros estritos (findByStatusIn...) quando texto for nulo")
    void deveBuscarComFiltrosEstritosQuandoTextoForNulo() {
        // Arrange
        List<SolicitationStatus> statuses = List.of(SolicitationStatus.APPROVED);
        List<String> states = List.of("RJ");
        ServiceType service = ServiceType.INSTALLATION;
        Priority priority = Priority.HIGH;
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        Page<SolicitationDocument> expectedPage = new PageImpl<>(List.of(solicitationDocument));

        when(elasticsearchRepository.findByStatusInAndStateInAndServiceTypeAndPriorityAndCreatedAtBetween(
                statuses, states, service, priority, from, to, pageable
        )).thenReturn(expectedPage);

        // Act
        Page<SolicitationDocument> result = elasticsearchService.searchSolicitations(
                null, statuses, states, service, priority, from, to, pageable
        );

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(elasticsearchRepository).findByStatusInAndStateInAndServiceTypeAndPriorityAndCreatedAtBetween(
                statuses, states, service, priority, from, to, pageable
        );
    }

    @Test
    @DisplayName("Deve retornar Page.empty() caso lance exceção durante a busca (try/catch)")
    void deveRetornarPaginaVaziaAoDarErroNaBusca() {
        // Arrange
        when(elasticsearchRepository.searchWithFilters(anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("Cluster Elasticsearch caiu"));

        // Act
        Page<SolicitationDocument> result = elasticsearchService.searchSolicitations(
                "teste", null, null, null, null, null, null, pageable
        );

        // Assert
        assertThat(result).isEmpty();
    }
}
