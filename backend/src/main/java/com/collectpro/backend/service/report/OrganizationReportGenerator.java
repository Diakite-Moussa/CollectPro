package com.collectpro.backend.service.report;

import com.collectpro.backend.dto.report.ReportData;
import com.collectpro.backend.service.FileStorageService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrganizationReportGenerator implements ReportPdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FileStorageService fileStorageService;

    @Override
    public byte[] generate(ReportData data) throws IOException {
        Document document = new Document(PageSize.A4, ReportPdfStyle.MARGIN, ReportPdfStyle.MARGIN, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new ReportPageFooter());
            document.open();

            addHeader(document, data);
            addSummary(document, data);
            addAgentsTable(document, data.agentsActivity());
            addDailyTrend(document, data.dailyTrend());

        } catch (DocumentException e) {
            throw new IOException("Erreur lors de la génération du PDF", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, ReportData data) throws DocumentException, IOException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1, 4});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String storedLogoName = data.organization() != null ? data.organization().logoStoredFilename() : null;
        if (storedLogoName != null) {
            try {
                Path path = fileStorageService.resolvePath(storedLogoName);
                if (Files.exists(path)) {
                    Image logo = Image.getInstance(path.toString());
                    logo.scaleToFit(ReportPdfStyle.LOGO_SIZE, ReportPdfStyle.LOGO_SIZE);
                    logoCell.addElement(logo);
                }
            } catch (Exception ignored) {
                // Logo illisible ou absent -> template neutre, on continue sans logo
            }
        }
        header.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.addElement(new Paragraph(data.organization().name(), ReportPdfStyle.titleFont()));
        titleCell.addElement(new Paragraph("Rapport d'organisation", ReportPdfStyle.mutedFont()));
        String period = periodLabel(data);
        titleCell.addElement(new Paragraph(period, ReportPdfStyle.mutedFont()));
        header.addCell(titleCell);

        document.add(header);
        document.add(Chunk.NEWLINE);
    }

    private void addSummary(Document document, ReportData data) throws DocumentException {
        document.add(new Paragraph("Résumé global", ReportPdfStyle.sectionFont()));
        document.add(Chunk.NEWLINE);

        var kpis = data.kpis();
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        addKpiCell(table, "Total collectes", String.valueOf(kpis.totalCollectes()));
        addKpiCell(table, "Validées", kpis.validatedCollectes() + " (" + kpis.validationRate() + "%)");
        addKpiCell(table, "Rejetées", kpis.rejectedCollectes() + " (" + kpis.rejectionRate() + "%)");
        addKpiCell(table, "Temps moyen validation", kpis.avgValidationTimeHours() + " h");

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addKpiCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(ReportPdfStyle.COLOR_BORDER);
        cell.setPadding(8);
        cell.addElement(new Paragraph(value, ReportPdfStyle.sectionFont()));
        cell.addElement(new Paragraph(label, ReportPdfStyle.mutedFont()));
        table.addCell(cell);
    }

    private void addAgentsTable(Document document, List<ReportData.AgentActivitySummary> agents) throws DocumentException {
        document.add(new Paragraph("Rejets par agent", ReportPdfStyle.sectionFont()));
        document.add(Chunk.NEWLINE);

        if (agents.isEmpty()) {
            document.add(new Paragraph("Aucune donnée disponible pour la période.", ReportPdfStyle.mutedFont()));
            document.add(Chunk.NEWLINE);
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1, 1, 1});

        addTableHeaderRow(table, "Agent", "Total", "Validées", "Rejetées", "En attente");

        for (var agent : agents) {
            addBodyCell(table, agent.agentName());
            addBodyCell(table, String.valueOf(agent.total()));
            addBodyCell(table, String.valueOf(agent.validated()));
            addBodyCell(table, String.valueOf(agent.rejected()));
            addBodyCell(table, String.valueOf(agent.pending()));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addDailyTrend(Document document, List<ReportData.DailyTrendPoint> trend) throws DocumentException {
        document.add(new Paragraph("Tendance journalière", ReportPdfStyle.sectionFont()));
        document.add(Chunk.NEWLINE);

        if (trend.isEmpty()) {
            document.add(new Paragraph("Aucune donnée disponible pour la période.", ReportPdfStyle.mutedFont()));
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setWidths(new float[]{2, 1});
        addTableHeaderRow(table, "Date", "Collectes");

        for (var point : trend) {
            addBodyCell(table, point.date().format(DATE_FMT));
            addBodyCell(table, String.valueOf(point.collectesCount()));
        }

        document.add(table);
    }

    private void addTableHeaderRow(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, ReportPdfStyle.tableHeaderFont()));
            cell.setBackgroundColor(ReportPdfStyle.COLOR_PRIMARY);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addBodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ReportPdfStyle.tableCellFont()));
        cell.setPadding(6);
        cell.setBorderColor(ReportPdfStyle.COLOR_BORDER);
        table.addCell(cell);
    }

    private String periodLabel(ReportData data) {
        var period = data.period();
        if (period == null || period.start() == null || period.end() == null) {
            return "Période : toutes données";
        }
        return "Période : " + period.start().toLocalDate().format(DATE_FMT)
                + " au " + period.end().toLocalDate().format(DATE_FMT);
    }
}