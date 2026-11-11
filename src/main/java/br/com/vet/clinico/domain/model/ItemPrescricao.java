package br.com.vet.clinico.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_prescricao")
@Getter
@Setter
@NoArgsConstructor
public class ItemPrescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescricao_id", nullable = false)
    private Long prescricaoId;

    @Column(name = "medicamento_id", nullable = false)
    private Long medicamentoId;

    @Column(name = "nome_medicamento", nullable = false, length = 100)
    private String nomeMedicamento;

    @Column(name = "dosagem_concentracao", nullable = false, length = 100)
    private String dosagemConcentracao;

    @Column(nullable = false, length = 255)
    private String posologia;

    @Column(name = "duracao_tratamento", length = 50)
    private String duracaoTratamento;

    @Column(name = "via_administracao", length = 50)
    private String viaAdministracao;

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
