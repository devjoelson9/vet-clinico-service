package br.com.vet.clinico.web.controller;

import br.com.vet.clinico.application.dto.request.RegistrarItemAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.ItemAtendimentoResponse;
import br.com.vet.clinico.application.service.ItemAtendimentoService;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/atendimentos/{atendimentoId}/itens")
@RequiredArgsConstructor
public class ItemAtendimentoController {

    private final ItemAtendimentoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<ItemAtendimentoResponse> registrar(
            @PathVariable Long atendimentoId,
            @Valid @RequestBody RegistrarItemAtendimentoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.status(201).body(service.registrar(atendimentoId, request, usuario));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<List<ItemAtendimentoResponse>> listar(@PathVariable Long atendimentoId) {
        return ResponseEntity.ok(service.listar(atendimentoId));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<Void> excluir(
            @PathVariable Long atendimentoId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        service.excluir(atendimentoId, itemId, usuario);
        return ResponseEntity.noContent().build();
    }
}
