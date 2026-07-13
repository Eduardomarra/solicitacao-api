package br.com.solicitacao.infrastructure.elasticsearch.mapper;

import br.com.solicitacao.infrastructure.elasticsearch.document.SolicitationDocument;
import br.com.solicitacao.infrastructure.persistence.entity.SolicitationEntity;
import br.com.solicitacao.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class SolicitationDocumentMapper {

    public SolicitationDocument toDocument(SolicitationEntity entity, String clientName) {
        if (entity == null) {
            return null;
        }

        return SolicitationDocument.builder()
                .id(entity.getId().toString())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .status(entity.getStatus())
                .serviceType(entity.getServiceType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .city(entity.getCity())
                .state(entity.getState())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}