package br.com.solicitacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitationStep2Request {

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido (formato: 00000-000)")
    private String cep;

    @NotBlank(message = "Número é obrigatório")
    @Size(min = 1, max = 20, message = "Número deve ter entre 1 e 20 caracteres")
    private String number;

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complement;

    private String street;
    private String neighborhood;
    private String city;

    @Pattern(regexp = "^[A-Z]{2}$", message = "UF deve ser 2 letras maiúsculas")
    private String state;
}