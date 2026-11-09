package br.com.vet.clinico.domain.model;

import br.com.vet.clinico.domain.enums.StatusAtendimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "atendimento")
@Getter
@Setter
@NoArgsConstructor
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agendamento_id")
    private Long agendamentoId;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Column(name = "veterinario_id", nullable = false)
    private Long veterinarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtendimento status = StatusAtendimento.EM_ANDAMENTO;

    @Column(name = "peso_kg", precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "queixa_sintomas", columnDefinition = "TEXT")
    private String queixaSintomas;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(columnDefinition = "TEXT")
    private String tratamento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(columnDefinition = "TEXT")
    private String orientacoes;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "valor_base", precision = 10, scale = 2)
    private BigDecimal valorBase;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
