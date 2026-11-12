
package br.com.vet.clinico.domain.model;

import br.com.vet.clinico.domain.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atendimento_id", nullable = false)
    private Long atendimentoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @PrePersist
    public void prePersist() {
        this.registradoEm = LocalDateTime.now();
    }
}
