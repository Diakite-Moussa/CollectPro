package com.collectpro.backend.service.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportPageFooter extends PdfPageEventHelper {

    private static final DateTimeFormatter GEN_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final String generatedAt = "Généré le " + LocalDateTime.now().format(GEN_DATE_FMT);

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        Phrase dateChunk = new Phrase(generatedAt, ReportPdfStyle.mutedFont());
        Phrase pageChunk = new Phrase("Page " + writer.getPageNumber(), ReportPdfStyle.mutedFont());

        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                dateChunk, document.left(), document.bottom() - 20, 0);

        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                pageChunk, document.right(), document.bottom() - 20, 0);
    }
}
