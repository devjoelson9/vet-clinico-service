package br.com.vet.clinico.web.controller;

import br.com.vet.clinico.application.dto.request.AtualizarAtendimentoRequest;
import br.com.vet.clinico.application.dto.request.IniciarAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.AtendimentoResponse;
import br.com.vet.clinico.application.dto.response.HistoricoClinicoResponse;
import br.com.vet.clinico.application.service.AtendimentoService;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/atendimentos")
@RequiredArgsConstructor
public class AtendimentoController {

    private final AtendimentoService service;

    @PostMapping("/iniciar")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<AtendimentoResponse> iniciar(
            @Valid @RequestBody IniciarAtendimentoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.status(201).body(service.iniciar(request, usuario));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<Page<AtendimentoResponse>> listar(
            @RequestParam(required = false) Long animalId,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) StatusAtendimento status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @PageableDefault(size = 20, sort = "iniciadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(service.listar(animalId, veterinarioId, status, inicio, fim, pageable));
    }

    @GetMapping("/historico/{animalId}")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<Page<HistoricoClinicoResponse>> consultarHistorico(
            @PathVariable Long animalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @PageableDefault(size = 10, sort = "finalizadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(service.consultarHistorico(animalId, inicio, fim, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<AtendimentoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<AtendimentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarAtendimentoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(service.atualizar(id, request, usuario));
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<AtendimentoResponse> finalizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarAtendimentoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(service.finalizar(id, request, usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        service.cancelar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
