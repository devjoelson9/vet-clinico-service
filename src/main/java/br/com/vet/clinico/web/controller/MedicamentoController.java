package br.com.vet.clinico.web.controller;

import br.com.vet.clinico.application.dto.request.AtualizarMedicamentoRequest;
import br.com.vet.clinico.application.dto.request.MedicamentoRequest;
import br.com.vet.clinico.application.dto.request.StatusItemRequest;
import br.com.vet.clinico.application.dto.response.MedicamentoResponse;
import br.com.vet.clinico.application.service.MedicamentoService;
import br.com.vet.clinico.domain.enums.StatusItem;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medicamentos")
@RequiredArgsConstructor
public class MedicamentoController {

    private final MedicamentoService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponse> cadastrar(@Valid @RequestBody MedicamentoRequest request) {
        return ResponseEntity.status(201).body(service.cadastrar(request));
    }

    @GetMapping
    public ResponseEntity<Page<MedicamentoResponse>> listar(
            @RequestParam(required = false) StatusItem status,
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable
    ) {
        return ResponseEntity.ok(service.listar(status, nome, pageable));
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<MedicamentoResponse>> listarAtivos() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicamentoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarMedicamentoRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponse> alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusItemRequest request
    ) {
        return ResponseEntity.ok(service.alterarStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
