package br.com.solicitacao.api.dto.request;

import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitationSearchRequest {

    private String text;

    private List<SolicitationStatus> statuses;

    private List<String> states;

    private ServiceType serviceType;

    private Priority priority;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;
}