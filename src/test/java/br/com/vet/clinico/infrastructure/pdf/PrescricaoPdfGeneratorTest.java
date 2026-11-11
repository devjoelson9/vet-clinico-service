package br.com.vet.clinico.infrastructure.pdf;

import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.model.ItemPrescricao;
import br.com.vet.clinico.domain.model.Prescricao;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescricaoPdfGeneratorTest {

    private final PrescricaoPdfGenerator generator = new PrescricaoPdfGenerator();

    @Test
    void deveGerarPdfComMedicamentosEOrientacoes() throws Exception {
        byte[] pdf = generator.gerar(atendimento(), prescricao(), itens());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");

        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(texto)
                    .contains("Receituário Veterinário")
                    .contains("Dipirona")
                    .contains("500 mg")
                    .contains("1 comprimido a cada 8 horas")
                    .contains("5 dias")
                    .contains("Oral")
                    .contains("Administrar após a alimentação");
        }
    }

    @Test
    void deveGerarPdfSemOrientacoesAdicionais() throws Exception {
        Prescricao prescricao = prescricao();
        prescricao.setOrientacoesGerais(null);

        byte[] pdf = generator.gerar(atendimento(), prescricao, itens());

        try (PdfReader reader = new PdfReader(pdf)) {
            String texto = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(texto).contains("Sem orientações adicionais.");
        }
    }

    private Atendimento atendimento() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(1L);
        atendimento.setAnimalId(2L);
        atendimento.setVeterinarioId(10L);
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        return atendimento;
    }

    private Prescricao prescricao() {
        Prescricao prescricao = new Prescricao();
        prescricao.setId(50L);
        prescricao.setAtendimentoId(1L);
        prescricao.setDataEmissao(LocalDate.of(2026, 11, 10));
        prescricao.setOrientacoesGerais("Administrar após a alimentação");
        return prescricao;
    }

    private List<ItemPrescricao> itens() {
        ItemPrescricao item = new ItemPrescricao();
        item.setId(9L);
        item.setPrescricaoId(50L);
        item.setMedicamentoId(7L);
        item.setNomeMedicamento("Dipirona");
        item.setDosagemConcentracao("500 mg");
        item.setPosologia("1 comprimido a cada 8 horas");
        item.setDuracaoTratamento("5 dias");
        item.setViaAdministracao("Oral");
        return List.of(item);
    }
}
