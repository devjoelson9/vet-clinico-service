package br.com.vet.clinico.domain.repository;

import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.enums.TipoItemCatalogo;
import br.com.vet.clinico.domain.model.CatalogoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CatalogoItemRepository extends JpaRepository<CatalogoItem, Long>, JpaSpecificationExecutor<CatalogoItem> {

    Optional<CatalogoItem> findByIdAndDeletadoEmIsNull(Long id);

    boolean existsByTipoAndNomeIgnoreCaseAndDeletadoEmIsNull(TipoItemCatalogo tipo, String nome);

    boolean existsByTipoAndNomeIgnoreCaseAndDeletadoEmIsNullAndIdNot(TipoItemCatalogo tipo, String nome, Long id);

    List<CatalogoItem> findByDeletadoEmIsNullAndStatusOrderByNomeAsc(StatusItem status);

    List<CatalogoItem> findByDeletadoEmIsNullAndStatusAndTipoOrderByNomeAsc(StatusItem status, TipoItemCatalogo tipo);
}
