
package br.com.vet.clinico.domain.repository;

import br.com.vet.clinico.domain.enums.StatusPagamento;
import br.com.vet.clinico.domain.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByAtendimentoIdOrderByRegistradoEmAsc(
            Long atendimentoId
    );

    Optional<Pagamento> findByIdAndAtendimentoId(
            Long id,
            Long atendimentoId
    );

    @Query("""
            SELECT COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE p.atendimentoId = :atendimentoId
            AND p.status = :status
            """)
    BigDecimal somarPorAtendimentoEStatus(
            @Param("atendimentoId") Long atendimentoId,
            @Param("status") StatusPagamento status
    );
}
