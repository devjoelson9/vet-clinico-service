package br.com.vet.clinico.web.controller;

import br.com.vet.clinico.application.dto.request.DefinirValorBaseRequest;
import br.com.vet.clinico.application.dto.response.ValorAtendimentoResponse;
import br.com.vet.clinico.application.service.ValorAtendimentoService;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atendimentos")
@RequiredArgsConstructor
public class ValorAtendimentoController {

    private final ValorAtendimentoService service;

    @GetMapping("/{id}/valor")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<ValorAtendimentoResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }

    @PatchMapping("/{id}/valor-base")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<ValorAtendimentoResponse> definirValorBase(
            @PathVariable Long id,
            @Valid @RequestBody DefinirValorBaseRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(service.definirValorBase(id, request, usuario));
    }
}
