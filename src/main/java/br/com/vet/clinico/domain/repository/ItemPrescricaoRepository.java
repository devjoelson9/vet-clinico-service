package br.com.vet.clinico.domain.repository;

import br.com.vet.clinico.domain.model.ItemPrescricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemPrescricaoRepository extends JpaRepository<ItemPrescricao, Long> {

    Optional<ItemPrescricao> findByIdAndPrescricaoIdAndDeletadoEmIsNull(Long id, Long prescricaoId);

    List<ItemPrescricao> findByPrescricaoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(Long prescricaoId);
}
