package com.collectpro.backend.service.report;

import com.collectpro.backend.dto.report.ReportData;
import java.io.IOException;

public interface ReportPdfGenerator {
    byte[] generate(ReportData data) throws IOException;
}