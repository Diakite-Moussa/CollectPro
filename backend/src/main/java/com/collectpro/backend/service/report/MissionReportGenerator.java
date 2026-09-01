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
public class MissionReportGenerator implements ReportPdfGenerator {

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
            addObjective(document, data);
            addAgentsTable(document, data.agentsActivity());

        } catch (DocumentException e) {
            throw new IOException("Erreur lors de la génération du PDF", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, ReportData data) throws DocumentException, IOException {
        var mission = data.mission();

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
                // Template neutre si logo illisible/absent
            }
        }
        header.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.addElement(new Paragraph(data.organization().name(), ReportPdfStyle.mutedFont()));
        titleCell.addElement(new Paragraph(mission.name(), ReportPdfStyle.titleFont()));
        titleCell.addElement(new Paragraph(periodLabel(mission), ReportPdfStyle.mutedFont()));
        header.addCell(titleCell);

        document.add(header);
        document.add(Chunk.NEWLINE);
    }

    private void addObjective(Document document, ReportData data) throws DocumentException {
        var mission = data.mission();
        var kpis = data.kpis();

        document.add(new Paragraph("Objectif vs reçues", ReportPdfStyle.sectionFont()));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);

        String expected = mission.expectedCollectes() != null
                ? String.valueOf(mission.expectedCollectes())
                : "Non défini";
        addKpiCell(table, "Objectif", expected);
        addKpiCell(table, "Collectes reçues", String.valueOf(kpis.totalCollectes()));

        String progress = mission.expectedCollectes() != null && mission.expectedCollectes() > 0
                ? Math.round(kpis.totalCollectes() * 100.0 / mission.expectedCollectes()) + "%"
                : "N/A";
        addKpiCell(table, "Taux d'atteinte", progress);

        document.add(table);
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph(
                "Agents assignés : " + mission.assignedAgentsCount(),
                ReportPdfStyle.bodyFont()));
        document.add(Chunk.NEWLINE);
    }

    private void addAgentsTable(Document document, List<ReportData.AgentActivitySummary> agents) throws DocumentException {
        document.add(new Paragraph("Activité des agents", ReportPdfStyle.sectionFont()));
        document.add(Chunk.NEWLINE);

        if (agents.isEmpty()) {
            document.add(new Paragraph("Aucune donnée disponible pour cette mission.", ReportPdfStyle.mutedFont()));
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
    }

    private void addKpiCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(ReportPdfStyle.COLOR_BORDER);
        cell.setPadding(8);
        cell.addElement(new Paragraph(value, ReportPdfStyle.sectionFont()));
        cell.addElement(new Paragraph(label, ReportPdfStyle.mutedFont()));
        table.addCell(cell);
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

    private String periodLabel(ReportData.MissionReportInfo mission) {
        if (mission.startDate() == null) return "Dates non définies";
        String end = mission.endDate() != null ? mission.endDate().toLocalDate().format(DATE_FMT) : "en cours";
        return "Du " + mission.startDate().toLocalDate().format(DATE_FMT) + " au " + end;
    }
}