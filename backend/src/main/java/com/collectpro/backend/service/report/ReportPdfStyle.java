package com.collectpro.backend.service.report;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import java.awt.Color;

public final class ReportPdfStyle {

    private ReportPdfStyle() {}

    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);   // bleu CollectPro
    public static final Color COLOR_TEXT = new Color(30, 41, 59);
    public static final Color COLOR_MUTED = new Color(100, 116, 139);
    public static final Color COLOR_BORDER = new Color(226, 232, 240);
    public static final Color COLOR_SUCCESS = new Color(22, 163, 74);
    public static final Color COLOR_DANGER = new Color(220, 38, 38);

    public static final float MARGIN = 40f;
    public static final float LOGO_SIZE = 60f;

    public static Font titleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY);
    }

    public static Font sectionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_TEXT);
    }

    public static Font bodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_TEXT);
    }

    public static Font mutedFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_MUTED);
    }

    public static Font tableHeaderFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    }

    public static Font tableCellFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT);
    }
}