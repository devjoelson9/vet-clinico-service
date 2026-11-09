package br.com.vet.clinico.domain.repository;

import br.com.vet.clinico.domain.model.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long>, JpaSpecificationExecutor<Atendimento> {

    Optional<Atendimento> findByIdAndDeletadoEmIsNull(Long id);

    boolean existsByAgendamentoIdAndDeletadoEmIsNull(Long agendamentoId);
}
