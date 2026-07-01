package com.heartsync.reporting.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class PdfGenerationService {

    private static final Color DARK_BLUE  = new Color(13, 27, 64);
    private static final Color TEAL       = new Color(32, 178, 170);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color WARN_AMBER = new Color(255, 165, 0);

    private static final Font TITLE_FONT    = new Font(Font.HELVETICA, 16, Font.BOLD,   Color.WHITE);
    private static final Font HEADER_FONT   = new Font(Font.HELVETICA, 11, Font.BOLD,   DARK_BLUE);
    private static final Font BODY_FONT     = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font LABEL_FONT    = new Font(Font.HELVETICA,  9, Font.BOLD,   DARK_BLUE);
    private static final Font SMALL_FONT    = new Font(Font.HELVETICA,  8, Font.NORMAL, Color.GRAY);

    /**
     * Generates the CardioSync AI Integrated Clinical Report PDF.
     * Layout matches the report design shown in the presentation (slide 5).
     */
    public byte[] generate(ReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, data);
            addPatientInfo(doc, data);
            addClinicalSummary(doc, data);

            boolean hasEcg   = data.heartRate() != null;
            boolean hasAngio = data.angioRisk() != null;

            if (hasEcg)   addEcgSection(doc, data);
            if (hasAngio) addAngiogramSection(doc, data);
            if (hasEcg && hasAngio) addFusionInsight(doc, data);

            addRiskAssessment(doc, data);
            addFinalImpression(doc, data);
            addFooter(doc, data);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addHeader(Document doc, ReportData data) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(DARK_BLUE);
        titleCell.setPadding(12);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph title = new Paragraph();
        title.add(new Chunk("CardioSync AI", new Font(Font.HELVETICA, 14, Font.BOLD, TEAL)));
        title.add(new Chunk("   Integrated Clinical Report\n", TITLE_FONT));
        title.add(new Chunk("Multimodal Cardiovascular Diagnostic Summary",
                new Font(Font.HELVETICA, 9, Font.NORMAL, Color.LIGHT_GRAY)));
        titleCell.addElement(title);
        header.addCell(titleCell);
        doc.add(header);
        doc.add(Chunk.NEWLINE);
    }

    private void addPatientInfo(Document doc, ReportData data) throws DocumentException {
        doc.add(sectionHeader("Patient Information"));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1.5f, 1, 1.5f});

        addLabelValue(table, "Name:",       data.patientName());
        addLabelValue(table, "Patient ID:", data.patientId());
        addLabelValue(table, "Date of Birth:", data.dateOfBirth());
        addLabelValue(table, "Date of Visit:", data.visitDate());
        addLabelValue(table, "Gender:",     data.gender());
        addLabelValue(table, "Referring Physician:", data.referringPhysician());

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    private void addClinicalSummary(Document doc, ReportData data) throws DocumentException {
        doc.add(sectionHeader("Clinical Summary"));
        Paragraph p = new Paragraph(data.clinicalSummary(), BODY_FONT);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private void addEcgSection(Document doc, ReportData data) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        box.setSpacingBefore(4);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(TEAL);
        cell.setPadding(8);
        cell.addElement(new Paragraph("ECG Analysis Results", HEADER_FONT));
        cell.addElement(checkRow("Heart Rate:",   data.heartRate()   + " bpm"));
        cell.addElement(checkRow("Rhythm:",       data.rhythm()      != null ? data.rhythm()      : "—"));
        cell.addElement(checkRow("PR Interval:",  data.prInterval()  + " ms"));
        cell.addElement(checkRow("QRS Duration:", data.qrsDuration() + " ms"));
        cell.addElement(checkRow("QT Interval:",  data.qtInterval()  + " ms"));
        if (data.ecgFindings() != null) {
            cell.addElement(Chunk.NEWLINE);
            cell.addElement(new Paragraph("Findings: " + data.ecgFindings(), BODY_FONT));
        }
        if (data.coronaryFindings() != null && !data.coronaryFindings().isEmpty()) {
            cell.addElement(Chunk.NEWLINE);
            cell.addElement(new Paragraph("AI ECG Classification:", LABEL_FONT));
            for (Map.Entry<String, String> e : data.coronaryFindings().entrySet())
                cell.addElement(checkRow(e.getKey() + ":", e.getValue()));
            if (data.calciumScore() != null)
                cell.addElement(checkRow("Calcium Score:", data.calciumScore()));
            if (data.dominance() != null)
                cell.addElement(checkRow("Dominance:", data.dominance()));
        }
        box.addCell(cell);
        doc.add(box);
        doc.add(Chunk.NEWLINE);
    }

    private void addAngiogramSection(Document doc, ReportData data) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        box.setSpacingBefore(4);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(TEAL);
        cell.setPadding(8);
        cell.addElement(new Paragraph("AI Angiogram Analysis (Segmentation-Based)", HEADER_FONT));
        cell.addElement(checkRow("Overall Risk:", data.angioRisk()));
        if (data.angioConfidence() != null)
            cell.addElement(checkRow("Model Confidence:", String.format("%.1f%%", data.angioConfidence() * 100)));
        if (data.angioTotalBranches() != null)
            cell.addElement(checkRow("Branches Detected:", String.valueOf(data.angioTotalBranches())));
        if (data.angioStenosisPercent() != null)
            cell.addElement(checkRow("Max Stenosis (DS%):", data.angioStenosisPercent() + "%"));

        if (data.angioLesions() != null && !data.angioLesions().isEmpty()) {
            cell.addElement(Chunk.NEWLINE);
            cell.addElement(new Paragraph("Detected Lesions (" + data.angioLesions().size() + "):", LABEL_FONT));

            PdfPTable lesionTable = new PdfPTable(5);
            lesionTable.setWidthPercentage(100);
            lesionTable.setWidths(new float[]{0.5f, 1.5f, 1f, 1f, 1f});
            lesionTable.setSpacingBefore(4);
            for (String h : new String[]{"#", "Severity", "DS%", "MLD (px)", "Length (px)"}) {
                PdfPCell hc = new PdfPCell(new Paragraph(h, LABEL_FONT));
                hc.setBackgroundColor(LIGHT_GRAY);
                hc.setPadding(3);
                lesionTable.addCell(hc);
            }
            for (ReportingService.LesionData l : data.angioLesions()) {
                for (String v : new String[]{
                        String.valueOf(l.rank()),
                        l.severity(),
                        String.format("%.1f%%", l.dsPercent()),
                        String.format("%.1f", l.mldPx()),
                        String.format("%.1f", l.lengthPx())}) {
                    PdfPCell vc = new PdfPCell(new Paragraph(v, BODY_FONT));
                    vc.setPadding(3);
                    lesionTable.addCell(vc);
                }
            }
            cell.addElement(lesionTable);
        } else {
            cell.addElement(checkRow("Lesions:", "No significant lesions detected"));
        }

        box.addCell(cell);
        doc.add(box);
        doc.add(Chunk.NEWLINE);
    }

    private void addFusionInsight(Document doc, ReportData data) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(230, 245, 255));
        cell.setBorderColor(TEAL);
        cell.setPadding(10);

        cell.addElement(new Paragraph("Multimodal Fusion Insight (CardioSync AI Engine)", HEADER_FONT));
        Paragraph insight = new Paragraph(data.fusionInsight(), BODY_FONT);
        insight.setSpacingBefore(4);
        cell.addElement(insight);
        box.addCell(cell);
        doc.add(box);
        doc.add(Chunk.NEWLINE);
    }

    private void addRiskAssessment(Document doc, ReportData data) throws DocumentException {
        doc.add(sectionHeader("Risk Assessment"));
        Color riskColor = switch (data.overallRisk()) {
            case "HIGH"     -> Color.RED;
            case "MODERATE" -> WARN_AMBER;
            default         -> new Color(0, 150, 0);
        };

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addLabelValue(table, "Overall Cardiovascular Risk:",
                new Paragraph(data.overallRisk(), new Font(Font.HELVETICA, 9, Font.BOLD, riskColor)));
        addLabelValue(table, "Immediate Risk:", "Low");
        addLabelValue(table, "Recommendation:", "Routine monitoring");
        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    private void addFinalImpression(Document doc, ReportData data) throws DocumentException {
        doc.add(sectionHeader("Final Impression"));
        for (String line : data.finalImpression().split("\n")) {
            Paragraph p = new Paragraph("• " + line.trim(), BODY_FONT);
            p.setSpacingAfter(2);
            doc.add(p);
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addFooter(Document doc, ReportData data) throws DocumentException {
        String generated = "Generated by: CardioSync AI  |  Report Date: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        doc.add(new Paragraph(generated, SMALL_FONT));
        doc.add(new Paragraph("Report ID: " + data.reportId(), SMALL_FONT));
    }

    // --- helpers ---

    private Paragraph sectionHeader(String title) {
        Paragraph p = new Paragraph(title, HEADER_FONT);
        p.setSpacingBefore(6);
        p.setSpacingAfter(4);
        return p;
    }

    private void addLabelValue(PdfPTable table, String label, String value) {
        addLabelValue(table, label, new Paragraph(value, BODY_FONT));
    }

    private void addLabelValue(PdfPTable table, String label, Paragraph valueP) {
        PdfPCell l = new PdfPCell(new Paragraph(label, LABEL_FONT));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPadding(3);
        l.setBackgroundColor(LIGHT_GRAY);

        PdfPCell v = new PdfPCell(valueP);
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(3);

        table.addCell(l);
        table.addCell(v);
    }

    private Paragraph checkRow(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk("✓ ", new Font(Font.HELVETICA, 9, Font.BOLD, TEAL)));
        p.add(new Chunk(label + " ", LABEL_FONT));
        p.add(new Chunk(value, BODY_FONT));
        return p;
    }

    // DTO carrying all report data into the generator.
    // ECG fields are null when no ECG was uploaded.
    // Angio fields are null when no angiogram was uploaded.
    public record ReportData(
            String  reportId,
            String  patientId,
            String  patientName,
            String  dateOfBirth,
            String  gender,
            String  visitDate,
            String  referringPhysician,
            String  clinicalSummary,
            // ECG section (null = not available)
            Integer heartRate,
            String  rhythm,
            Integer prInterval,
            Integer qrsDuration,
            Integer qtInterval,
            String  ecgFindings,
            // AI ECG classification (null = not available)
            Map<String, String> coronaryFindings,
            Integer stenosisPercent,
            String  calciumScore,
            String  dominance,
            // Angiogram section (null = not available)
            String  angioRisk,
            Double  angioConfidence,
            Integer angioTotalBranches,
            Integer angioStenosisPercent,
            java.util.List<ReportingService.LesionData> angioLesions,
            // Summary
            String  fusionInsight,
            String  overallRisk,
            String  finalImpression
    ) {}
}
