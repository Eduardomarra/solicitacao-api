package br.com.solicitacao.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "analyst_coverage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalystCoverageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "state", nullable = false, length = 2)
    private String state;
}