package br.com.solicitacao.api.dto.request;

import br.com.solicitacao.core.domain.enums.Priority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
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
public class SolicitationStep3Request {

    @NotNull(message = "Prioridade é obrigatória")
    private Priority priority;

    @NotNull(message = "Data preferencial é obrigatória")
    @FutureOrPresent(message = "Data preferencial não pode ser no passado")
    private LocalDate preferredDate;

    @NotNull(message = "Valor estimado é obrigatório")
    @DecimalMin(value = "0.0", message = "Valor estimado deve ser maior ou igual a 0")
    private BigDecimal estimatedValue;

    @NotNull(message = "Termos devem ser aceitos")
    private Boolean termsAccepted;
}