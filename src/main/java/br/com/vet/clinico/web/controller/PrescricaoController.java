package br.com.vet.clinico.web.controller;

import br.com.vet.clinico.application.dto.request.AtualizarPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.ItemPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.RegistrarPrescricaoRequest;
import br.com.vet.clinico.application.dto.response.ItemPrescricaoResponse;
import br.com.vet.clinico.application.dto.response.PrescricaoResponse;
import br.com.vet.clinico.application.service.PrescricaoService;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atendimentos/{atendimentoId}/prescricao")
@RequiredArgsConstructor
public class PrescricaoController {

    private final PrescricaoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<PrescricaoResponse> registrar(
            @PathVariable Long atendimentoId,
            @Valid @RequestBody RegistrarPrescricaoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.status(201).body(service.registrar(atendimentoId, request, usuario));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<PrescricaoResponse> consultar(@PathVariable Long atendimentoId) {
        return ResponseEntity.ok(service.consultar(atendimentoId));
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<PrescricaoResponse> atualizar(
            @PathVariable Long atendimentoId,
            @Valid @RequestBody AtualizarPrescricaoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(service.atualizar(atendimentoId, request, usuario));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<Void> excluir(
            @PathVariable Long atendimentoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        service.excluir(atendimentoId, usuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/itens")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<ItemPrescricaoResponse> adicionarItem(
            @PathVariable Long atendimentoId,
            @Valid @RequestBody ItemPrescricaoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.status(201).body(service.adicionarItem(atendimentoId, request, usuario));
    }

    @DeleteMapping("/itens/{itemId}")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    public ResponseEntity<Void> excluirItem(
            @PathVariable Long atendimentoId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        service.excluirItem(atendimentoId, itemId, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pdf")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long atendimentoId) {
        byte[] pdf = service.gerarPdf(atendimentoId);
        String nomeArquivo = "prescricao-" + atendimentoId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(pdf);
    }
}
