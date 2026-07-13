package br.com.solicitacao.infrastructure.elasticsearch.repository;

import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.infrastructure.elasticsearch.document.SolicitationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SolicitationElasticsearchRepository extends ElasticsearchRepository<SolicitationDocument, String> {

    // Busca por texto em title e description
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title\", \"description\"]}}]}}")
    Page<SolicitationDocument> searchByText(String text, Pageable pageable);

    // Busca com filtros
    Page<SolicitationDocument> findByStatusInAndStateInAndServiceTypeAndPriorityAndCreatedAtBetween(
            List<SolicitationStatus> statuses,
            List<String> states,
            ServiceType serviceType,
            Priority priority,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    );

    // Busca com texto e filtros
    @Query("{\"bool\": {" +
            "\"must\": [" +
            "{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title\", \"description\"]}}" +
            "]," +
            "\"filter\": [" +
            "{\"terms\": {\"status\": ?1}}," +
            "{\"terms\": {\"state\": ?2}}" +
            "]}}")
    Page<SolicitationDocument> searchWithFilters(
            String text,
            List<SolicitationStatus> statuses,
            List<String> states,
            Pageable pageable
    );
}