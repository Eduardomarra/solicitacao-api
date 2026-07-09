package br.com.solicitacao.api.dto.request;

import br.com.solicitacao.core.domain.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitationStep1Request {

    @NotNull(message = "Tipo de serviço é obrigatório")
    private ServiceType serviceType;

    @NotBlank(message = "Título é obrigatório")
    @Size(min = 3, max = 80, message = "Título deve ter entre 3 e 80 caracteres")
    private String title;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 20, max = 1000, message = "Descrição deve ter entre 20 e 1000 caracteres")
    private String description;
}