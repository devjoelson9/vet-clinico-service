package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.AtualizarMedicamentoRequest;
import br.com.vet.clinico.application.dto.request.MedicamentoRequest;
import br.com.vet.clinico.application.dto.response.MedicamentoResponse;
import br.com.vet.clinico.application.exception.ConflictException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.mapper.MedicamentoMapper;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.model.Medicamento;
import br.com.vet.clinico.domain.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicamentoService {

    private final MedicamentoRepository repository;
    private final MedicamentoMapper mapper;

    @Transactional
    public MedicamentoResponse cadastrar(MedicamentoRequest request) {
        String nome = request.nome().trim();
        if (repository.existsByNomeIgnoreCaseAndDeletadoEmIsNull(nome)) {
            throw new ConflictException("Já existe um medicamento com o nome informado");
        }

        Medicamento medicamento = mapper.toEntity(request);
        return mapper.toResponse(repository.save(medicamento));
    }

    @Transactional(readOnly = true)
    public Page<MedicamentoResponse> listar(StatusItem status, String nome, Pageable pageable) {
        Specification<Medicamento> spec = (root, query, cb) -> cb.isNull(root.get("deletadoEm"));

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (nome != null && !nome.isBlank()) {
            String termo = "%" + nome.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), termo));
        }

        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MedicamentoResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarMedicamentoAtivo(id));
    }

    @Transactional(readOnly = true)
    public List<MedicamentoResponse> listarAtivos() {
        return repository.findByDeletadoEmIsNullAndStatusOrderByNomeAsc(StatusItem.ATIVO)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public MedicamentoResponse atualizar(Long id, AtualizarMedicamentoRequest request) {
        Medicamento medicamento = buscarMedicamentoAtivo(id);

        if (request.nome() != null && !request.nome().isBlank()) {
            String nome = request.nome().trim();
            if (repository.existsByNomeIgnoreCaseAndDeletadoEmIsNullAndIdNot(nome, id)) {
                throw new ConflictException("Já existe um medicamento com o nome informado");
            }
        }

        mapper.updateEntity(medicamento, request);
        return mapper.toResponse(repository.save(medicamento));
    }

    @Transactional
    public MedicamentoResponse alterarStatus(Long id, StatusItem status) {
        Medicamento medicamento = buscarMedicamentoAtivo(id);
        medicamento.setStatus(status);
        return mapper.toResponse(repository.save(medicamento));
    }

    @Transactional
    public void excluir(Long id) {
        Medicamento medicamento = buscarMedicamentoAtivo(id);
        medicamento.setDeletadoEm(LocalDateTime.now());
        medicamento.setStatus(StatusItem.INATIVO);
        repository.save(medicamento);
    }

    private Medicamento buscarMedicamentoAtivo(Long id) {
        return repository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento não encontrado: " + id));
    }
}
