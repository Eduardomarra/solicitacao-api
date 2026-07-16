package br.com.solicitacao.api.dto.request;

import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateSolicitationRequest {

    // Step 1
    private ServiceType serviceType;

    @Size(min = 3, max = 80, message = "Título deve ter entre 3 e 80 caracteres")
    private String title;

    @Size(min = 20, max = 1000, message = "Descrição deve ter entre 20 e 1000 caracteres")
    private String description;

    // Step 2
    private String cep;
    private String number;
    private String complement;
    private String street;
    private String neighborhood;
    private String city;
    private String state;

    // Step 3
    private Priority priority;

    @FutureOrPresent(message = "Data preferencial não pode ser no passado")
    private LocalDate preferredDate;

    @DecimalMin(value = "0.0", message = "Valor estimado deve ser maior ou igual a 0")
    private BigDecimal estimatedValue;

    private Boolean termsAccepted;

    // Submeter diretamente?
    private Boolean submitDirectly;
}