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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final SolicitationElasticsearchRepository elasticsearchRepository;
    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationDocumentMapper documentMapper;

    public void indexSolicitation(UUID solicitationId) {
        log.info("Indexando solicitação no Elasticsearch: {}", solicitationId);

        try {
            SolicitationEntity entity = solicitationRepository.findById(solicitationId).orElse(null);
            if (entity == null) {
                log.warn("Solicitação não encontrada: {}", solicitationId);
                return;
            }

            String clientName = userRepository.findById(entity.getClientId())
                    .map(UserEntity::getName)
                    .orElse("Usuário não encontrado");

            SolicitationDocument document = documentMapper.toDocument(entity, clientName);
            elasticsearchRepository.save(document);
            log.info("Solicitação indexada com sucesso: {}", solicitationId);
        } catch (Exception e) {
            log.error("Erro ao indexar solicitação: {}", e.getMessage(), e);
        }
    }

    public void deleteSolicitation(UUID solicitationId) {
        log.info("Removendo solicitação do Elasticsearch: {}", solicitationId);
        try {
            elasticsearchRepository.deleteById(solicitationId.toString());
            log.info("Solicitação removida com sucesso: {}", solicitationId);
        } catch (Exception e) {
            log.error("Erro ao remover solicitação: {}", e.getMessage(), e);
        }
    }

    public Page<SolicitationDocument> searchSolicitations(
            String text,
            List<SolicitationStatus> statuses,
            List<String> states,
            ServiceType serviceType,
            Priority priority,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable) {

        log.info("Buscando solicitações no Elasticsearch - text: {}, statuses: {}, states: {}",
                text, statuses, states);

        try {
            if (text != null && !text.isEmpty()) {
                // Busca com texto
                return elasticsearchRepository.searchWithFilters(
                        text,
                        statuses,
                        states,
                        pageable
                );
            } else {
                // Busca com filtros
                return elasticsearchRepository.findByStatusInAndStateInAndServiceTypeAndPriorityAndCreatedAtBetween(
                        statuses,
                        states,
                        serviceType,
                        priority,
                        dateFrom,
                        dateTo,
                        pageable
                );
            }
        } catch (Exception e) {
            log.error("Erro ao buscar solicitações: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }
}