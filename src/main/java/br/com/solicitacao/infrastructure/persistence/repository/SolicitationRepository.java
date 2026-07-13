package br.com.solicitacao.infrastructure.persistence.repository;

import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SolicitationRepository extends JpaRepository<SolicitationEntity, UUID> {

    // Buscar por cliente
    Page<SolicitationEntity> findByClientId(UUID clientId, Pageable pageable);

    List<SolicitationEntity> findByClientIdAndStatus(UUID clientId, SolicitationStatus status);

    // Buscar por status
    Page<SolicitationEntity> findByStatus(SolicitationStatus status, Pageable pageable);

    // Buscar por lista de UFs e status (para analista)
    Page<SolicitationEntity> findByStateInAndStatus(List<String> states, SolicitationStatus status, Pageable pageable);

    // Buscar por lista de UFs e status diferente (para analista - todas exceto DRAFT)
    Page<SolicitationEntity> findByStateInAndStatusNot(List<String> states, SolicitationStatus status, Pageable pageable);

    // Verificar se solicitação pertence ao cliente
    boolean existsByIdAndClientId(UUID id, UUID clientId);
}