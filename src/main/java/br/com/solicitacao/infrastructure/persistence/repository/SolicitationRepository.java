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

    Page<SolicitationEntity> findByClientId(UUID clientId, Pageable pageable);

    List<SolicitationEntity> findByClientIdAndStatus(UUID clientId, SolicitationStatus status);

    Page<SolicitationEntity> findByStatus(SolicitationStatus status, Pageable pageable);

    Page<SolicitationEntity> findByStateInAndStatus(List<String> states, SolicitationStatus status, Pageable pageable);

    Page<SolicitationEntity> findByStateInAndStatusNot(List<String> states, SolicitationStatus status, Pageable pageable);

    boolean existsByIdAndClientId(UUID id, UUID clientId);

    // ============================================
    // COUNT METHODS FOR DASHBOARD
    // ============================================

    long countByStatus(SolicitationStatus status);
}