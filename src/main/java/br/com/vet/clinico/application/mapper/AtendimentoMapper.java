package br.com.vet.clinico.application.mapper;

import br.com.vet.clinico.application.dto.request.AtualizarAtendimentoRequest;
import br.com.vet.clinico.application.dto.request.IniciarAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.AtendimentoResponse;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.model.Atendimento;
import org.springframework.stereotype.Component;

@Component
public class AtendimentoMapper {

    public Atendimento toEntity(IniciarAtendimentoRequest request, Long veterinarioId) {
        Atendimento atendimento = new Atendimento();
        atendimento.setAgendamentoId(request.agendamentoId());
        atendimento.setAnimalId(request.animalId());
        atendimento.setVeterinarioId(veterinarioId);
        atendimento.setPesoKg(request.pesoKg());
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        atendimento.setIniciadoEm(java.time.LocalDateTime.now());
        return atendimento;
    }

    public void updateEntity(Atendimento atendimento, AtualizarAtendimentoRequest request) {
        if (request.pesoKg() != null) {
            atendimento.setPesoKg(request.pesoKg());
        }
        if (request.queixaSintomas() != null) {
            atendimento.setQueixaSintomas(normalizarTexto(request.queixaSintomas()));
        }
        if (request.diagnostico() != null) {
            atendimento.setDiagnostico(normalizarTexto(request.diagnostico()));
        }
        if (request.tratamento() != null) {
            atendimento.setTratamento(normalizarTexto(request.tratamento()));
        }
        if (request.observacoes() != null) {
            atendimento.setObservacoes(normalizarTexto(request.observacoes()));
        }
        if (request.orientacoes() != null) {
            atendimento.setOrientacoes(normalizarTexto(request.orientacoes()));
        }
    }

    public AtendimentoResponse toResponse(Atendimento atendimento) {
        return new AtendimentoResponse(
                atendimento.getId(),
                atendimento.getAgendamentoId(),
                atendimento.getAnimalId(),
                atendimento.getVeterinarioId(),
                atendimento.getStatus(),
                atendimento.getPesoKg(),
                atendimento.getQueixaSintomas(),
                atendimento.getDiagnostico(),
                atendimento.getTratamento(),
                atendimento.getObservacoes(),
                atendimento.getOrientacoes(),
                atendimento.getIniciadoEm(),
                atendimento.getFinalizadoEm(),
                atendimento.getValorBase(),
                atendimento.getValorTotal(),
                atendimento.getCriadoEm(),
                atendimento.getAtualizadoEm()
        );
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = texto.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
