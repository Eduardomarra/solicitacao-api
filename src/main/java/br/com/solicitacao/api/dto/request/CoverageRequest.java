package br.com.solicitacao.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageRequest {

    private UUID userId;

    @NotEmpty(message = "Pelo menos um estado deve ser informado")
    private List<String> ufs;
}