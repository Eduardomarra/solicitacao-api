package br.com.solicitacao.infrastructure.persistence.repository;

import br.com.solicitacao.infrastructure.persistence.entity.AnalystCoverageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalystCoverageRepository extends JpaRepository<AnalystCoverageEntity, UUID> {

    List<AnalystCoverageEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    boolean existsByUserIdAndState(UUID userId, String state);
}