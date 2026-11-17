
package br.com.vet.clinico.web.controller;

import br.com.vet.clinico.application.dto.request.RegistrarPagamentoRequest;
import br.com.vet.clinico.application.dto.response.PagamentoResponse;
import br.com.vet.clinico.application.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/atendimentos/{id}/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<PagamentoResponse> registrar(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarPagamentoRequest request
    ) {
        return ResponseEntity.status(201)
                .body(service.registrar(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<List<PagamentoResponse>> listar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.listar(id));
    }

    @PatchMapping("/{pagamentoId}/confirmar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<PagamentoResponse> confirmar(
            @PathVariable Long id,
            @PathVariable Long pagamentoId
    ) {
        return ResponseEntity.ok(
                service.confirmar(id, pagamentoId)
        );
    }
}
