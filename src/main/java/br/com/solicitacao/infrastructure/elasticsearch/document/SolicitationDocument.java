package br.com.solicitacao.infrastructure.elasticsearch.document;

import br.com.solicitacao.core.domain.enums.Priority;
import br.com.solicitacao.core.domain.enums.ServiceType;
import br.com.solicitacao.core.domain.enums.SolicitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "solicitations")
public class SolicitationDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private UUID clientId;

    @Field(type = FieldType.Text)
    private String clientName;

    @Field(type = FieldType.Keyword)
    private SolicitationStatus status;

    @Field(type = FieldType.Keyword)
    private ServiceType serviceType;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Text)
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Keyword)
    private Priority priority;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime submittedAt;
}