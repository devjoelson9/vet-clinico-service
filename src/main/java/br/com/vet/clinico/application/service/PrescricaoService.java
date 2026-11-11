package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.AtualizarPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.ItemPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.RegistrarPrescricaoRequest;
import br.com.vet.clinico.application.dto.response.ItemPrescricaoResponse;
import br.com.vet.clinico.application.dto.response.PrescricaoResponse;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ConflictException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.mapper.PrescricaoMapper;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.model.ItemPrescricao;
import br.com.vet.clinico.domain.model.Medicamento;
import br.com.vet.clinico.domain.model.Prescricao;
import br.com.vet.clinico.domain.repository.AtendimentoRepository;
import br.com.vet.clinico.domain.repository.ItemPrescricaoRepository;
import br.com.vet.clinico.domain.repository.MedicamentoRepository;
import br.com.vet.clinico.domain.repository.PrescricaoRepository;
import br.com.vet.clinico.infrastructure.pdf.PrescricaoPdfGenerator;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PrescricaoService {

    private final PrescricaoRepository prescricaoRepository;
    private final ItemPrescricaoRepository itemPrescricaoRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final PrescricaoMapper mapper;
    private final PrescricaoPdfGenerator pdfGenerator;

    @Transactional
    public PrescricaoResponse registrar(
            Long atendimentoId,
            RegistrarPrescricaoRequest request,
            UsuarioAutenticado usuario
    ) {
        buscarAtendimentoEmAndamento(atendimentoId, usuario);

        if (prescricaoRepository.existsByAtendimentoIdAndDeletadoEmIsNull(atendimentoId)) {
            throw new ConflictException("Atendimento já possui prescrição ativa");
        }

        Prescricao prescricao = prescricaoRepository.save(mapper.toEntity(atendimentoId, request));

        List<ItemPrescricao> itens = request.itens().stream()
                .map(item -> criarItem(prescricao.getId(), item))
                .toList();

        return mapper.toResponse(prescricao, itemPrescricaoRepository.saveAll(itens));
    }

    @Transactional(readOnly = true)
    public PrescricaoResponse consultar(Long atendimentoId) {
        validarAtendimentoExiste(atendimentoId);
        Prescricao prescricao = buscarPrescricaoAtiva(atendimentoId);
        return mapper.toResponse(prescricao, buscarItens(prescricao.getId()));
    }

    @Transactional
    public PrescricaoResponse atualizar(
            Long atendimentoId,
            AtualizarPrescricaoRequest request,
            UsuarioAutenticado usuario
    ) {
        buscarAtendimentoEmAndamento(atendimentoId, usuario);
        Prescricao prescricao = buscarPrescricaoAtiva(atendimentoId);

        mapper.updateEntity(prescricao, request);
        Prescricao salva = prescricaoRepository.save(prescricao);

        return mapper.toResponse(salva, buscarItens(salva.getId()));
    }

    @Transactional
    public ItemPrescricaoResponse adicionarItem(
            Long atendimentoId,
            ItemPrescricaoRequest request,
            UsuarioAutenticado usuario
    ) {
        buscarAtendimentoEmAndamento(atendimentoId, usuario);
        Prescricao prescricao = buscarPrescricaoAtiva(atendimentoId);

        ItemPrescricao item = criarItem(prescricao.getId(), request);
        return mapper.toResponse(itemPrescricaoRepository.save(item));
    }

    @Transactional
    public void excluirItem(Long atendimentoId, Long itemId, UsuarioAutenticado usuario) {
        buscarAtendimentoEmAndamento(atendimentoId, usuario);
        Prescricao prescricao = buscarPrescricaoAtiva(atendimentoId);

        ItemPrescricao item = itemPrescricaoRepository
                .findByIdAndPrescricaoIdAndDeletadoEmIsNull(itemId, prescricao.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item da prescrição não encontrado: " + itemId));

        item.setDeletadoEm(LocalDateTime.now());
        itemPrescricaoRepository.save(item);
    }

    @Transactional
    public void excluir(Long atendimentoId, UsuarioAutenticado usuario) {
        buscarAtendimentoEmAndamento(atendimentoId, usuario);
        Prescricao prescricao = buscarPrescricaoAtiva(atendimentoId);

        LocalDateTime agora = LocalDateTime.now();
        List<ItemPrescricao> itens = buscarItens(prescricao.getId());
        itens.forEach(item -> item.setDeletadoEm(agora));
        itemPrescricaoRepository.saveAll(itens);

        prescricao.setDeletadoEm(agora);
        prescricaoRepository.save(prescricao);
    }

    @Transactional(readOnly = true)
    public byte[] gerarPdf(Long atendimentoId) {
        Atendimento atendimento = validarAtendimentoExiste(atendimentoId);
        Prescricao prescricao = buscarPrescricaoAtiva(atendimentoId);
        return pdfGenerator.gerar(atendimento, prescricao, buscarItens(prescricao.getId()));
    }

    private ItemPrescricao criarItem(Long prescricaoId, ItemPrescricaoRequest request) {
        Medicamento medicamento = medicamentoRepository
                .findByIdAndDeletadoEmIsNull(request.medicamentoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medicamento não encontrado: " + request.medicamentoId()));

        if (medicamento.getStatus() != StatusItem.ATIVO) {
            throw new BusinessException("Medicamento está inativo e não pode ser utilizado");
        }

        return mapper.toEntity(prescricaoId, medicamento.getNome(), request);
    }

    private List<ItemPrescricao> buscarItens(Long prescricaoId) {
        return itemPrescricaoRepository
                .findByPrescricaoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(prescricaoId);
    }

    private Prescricao buscarPrescricaoAtiva(Long atendimentoId) {
        return prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(atendimentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prescrição não encontrada para o atendimento: " + atendimentoId));
    }

    private Atendimento validarAtendimentoExiste(Long atendimentoId) {
        return atendimentoRepository.findByIdAndDeletadoEmIsNull(atendimentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Atendimento não encontrado: " + atendimentoId));
    }

    private Atendimento buscarAtendimentoEmAndamento(Long atendimentoId, UsuarioAutenticado usuario) {
        Atendimento atendimento = validarAtendimentoExiste(atendimentoId);

        if (atendimento.getStatus() != StatusAtendimento.EM_ANDAMENTO) {
            throw new BusinessException("Atendimento não está em andamento");
        }
        if (!usuario.isAdmin() && !Objects.equals(atendimento.getVeterinarioId(), usuario.id())) {
            throw new AccessDeniedException("Apenas o veterinário responsável pode alterar este atendimento");
        }

        return atendimento;
    }
}
