package br.com.vet.clinico.infrastructure.pdf;

import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.model.ItemPrescricao;
import br.com.vet.clinico.domain.model.Prescricao;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PrescricaoPdfGenerator {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font TEXTO = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font TEXTO_NEGRITO = new Font(Font.HELVETICA, 10, Font.BOLD);

    public byte[] gerar(Atendimento atendimento, Prescricao prescricao, List<ItemPrescricao> itens) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            adicionarCabecalho(documento, atendimento, prescricao);
            adicionarItens(documento, itens);
            adicionarOrientacoes(documento, prescricao);
            adicionarRodape(documento);

            documento.close();
        } catch (DocumentException ex) {
            if (documento.isOpen()) {
                documento.close();
            }
            throw new BusinessException("Não foi possível gerar o PDF da prescrição");
        }

        return saida.toByteArray();
    }

    private void adicionarCabecalho(
            Document documento,
            Atendimento atendimento,
            Prescricao prescricao
    ) throws DocumentException {
        Paragraph titulo = new Paragraph("Receituário Veterinário", TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(16);
        documento.add(titulo);

        documento.add(new Paragraph("Atendimento: #" + atendimento.getId(), TEXTO));
        documento.add(new Paragraph("Animal: #" + atendimento.getAnimalId(), TEXTO));
        documento.add(new Paragraph("Veterinário responsável: #" + atendimento.getVeterinarioId(), TEXTO));
        documento.add(new Paragraph("Data de emissão: " + prescricao.getDataEmissao().format(DATA), TEXTO));

        Paragraph espaco = new Paragraph(" ");
        espaco.setSpacingAfter(8);
        documento.add(espaco);
    }

    private void adicionarItens(Document documento, List<ItemPrescricao> itens) throws DocumentException {
        Paragraph subtitulo = new Paragraph("Medicamentos prescritos", SUBTITULO);
        subtitulo.setSpacingAfter(6);
        documento.add(subtitulo);

        PdfPTable tabela = new PdfPTable(new float[]{3, 2, 4, 2, 2});
        tabela.setWidthPercentage(100);

        adicionarCabecalhoTabela(tabela, "Medicamento");
        adicionarCabecalhoTabela(tabela, "Dosagem/Concentração");
        adicionarCabecalhoTabela(tabela, "Posologia");
        adicionarCabecalhoTabela(tabela, "Duração");
        adicionarCabecalhoTabela(tabela, "Via");

        for (ItemPrescricao item : itens) {
            adicionarCelula(tabela, item.getNomeMedicamento());
            adicionarCelula(tabela, item.getDosagemConcentracao());
            adicionarCelula(tabela, item.getPosologia());
            adicionarCelula(tabela, item.getDuracaoTratamento());
            adicionarCelula(tabela, item.getViaAdministracao());
        }

        documento.add(tabela);
    }

    private void adicionarOrientacoes(Document documento, Prescricao prescricao) throws DocumentException {
        Paragraph espaco = new Paragraph(" ");
        espaco.setSpacingAfter(8);
        documento.add(espaco);

        Paragraph subtitulo = new Paragraph("Orientações gerais", SUBTITULO);
        subtitulo.setSpacingAfter(4);
        documento.add(subtitulo);

        String orientacoes = prescricao.getOrientacoesGerais();
        documento.add(new Paragraph(
                orientacoes == null || orientacoes.isBlank() ? "Sem orientações adicionais." : orientacoes,
                TEXTO
        ));
    }

    private void adicionarRodape(Document documento) throws DocumentException {
        Paragraph espaco = new Paragraph(" ");
        espaco.setSpacingBefore(24);
        documento.add(espaco);

        Paragraph rodape = new Paragraph(
                "Documento gerado eletronicamente pelo sistema de gestão clínica veterinária.",
                new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY)
        );
        rodape.setAlignment(Element.ALIGN_CENTER);
        documento.add(rodape);
    }

    private void adicionarCabecalhoTabela(PdfPTable tabela, String texto) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, TEXTO_NEGRITO));
        celula.setBackgroundColor(new Color(230, 230, 230));
        celula.setHorizontalAlignment(Element.ALIGN_LEFT);
        celula.setPadding(5);
        tabela.addCell(celula);
    }

    private void adicionarCelula(PdfPTable tabela, String texto) {
        PdfPCell celula = new PdfPCell(new Phrase(texto == null ? "-" : texto, TEXTO));
        celula.setPadding(5);
        tabela.addCell(celula);
    }
}
