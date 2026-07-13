package br.com.solicitacao.api.dto.response;

import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitationListResponse {
    private UUID id;
    private UUID clientId;
    private String clientName;
    private SolicitationStatus status;
    private Integer currentStep;
    private ServiceType serviceType;
    private String title;
    private String city;
    private String state;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
}