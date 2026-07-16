package br.com.solicitacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevisionRequest {

    @NotBlank(message = "Comentário é obrigatório")
    @Size(min = 10, max = 1000, message = "Comentário deve ter entre 10 e 1000 caracteres")
    private String comment;
}