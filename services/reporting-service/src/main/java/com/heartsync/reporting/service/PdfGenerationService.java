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
            addEcgResults(doc, data);
            addAiResults(doc, data);
            addFusionInsight(doc, data);
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

    private void addEcgResults(Document doc, ReportData data) throws DocumentException {
        PdfPTable columns = new PdfPTable(2);
        columns.setWidthPercentage(100);
        columns.setSpacingBefore(4);

        // Left: ECG
        PdfPCell ecgCell = new PdfPCell();
        ecgCell.setBorderColor(TEAL);
        ecgCell.setPadding(8);
        ecgCell.addElement(new Paragraph("ECG Analysis Results", HEADER_FONT));
        ecgCell.addElement(checkRow("Heart Rate:", data.heartRate() + " bpm"));
        ecgCell.addElement(checkRow("Rhythm:",     data.rhythm()));
        ecgCell.addElement(checkRow("PR Interval:", data.prInterval() + " ms"));
        ecgCell.addElement(checkRow("QRS Duration:", data.qrsDuration() + " ms"));
        ecgCell.addElement(checkRow("QT Interval:", data.qtInterval() + " ms"));
        columns.addCell(ecgCell);

        // Right: AI
        PdfPCell aiCell = new PdfPCell();
        aiCell.setBorderColor(TEAL);
        aiCell.setPadding(8);
        aiCell.addElement(new Paragraph("AI Angiogram Analysis (Segmentation-Based)", HEADER_FONT));
        aiCell.addElement(new Paragraph("Coronary Artery Findings:", LABEL_FONT));
        for (Map.Entry<String, String> entry : data.coronaryFindings().entrySet()) {
            aiCell.addElement(checkRow(entry.getKey() + ":", entry.getValue()));
        }
        aiCell.addElement(Chunk.NEWLINE);
        aiCell.addElement(new Paragraph("AI-Derived Metrics:", LABEL_FONT));
        aiCell.addElement(checkRow("Estimated Stenosis:", "< " + data.stenosisPercent() + "%"));
        aiCell.addElement(checkRow("Calcium Score:", data.calciumScore()));
        aiCell.addElement(checkRow("Dominance:", data.dominance()));
        columns.addCell(aiCell);

        doc.add(columns);
        doc.add(Chunk.NEWLINE);
    }

    private void addAiResults(Document doc, ReportData data) {
        // Already combined in addEcgResults
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

    // DTO carrying all report data into the generator
    public record ReportData(
            String reportId,
            String patientId,
            String patientName,
            String dateOfBirth,
            String gender,
            String visitDate,
            String referringPhysician,
            String clinicalSummary,
            int    heartRate,
            String rhythm,
            int    prInterval,
            int    qrsDuration,
            int    qtInterval,
            Map<String, String> coronaryFindings,
            int    stenosisPercent,
            String calciumScore,
            String dominance,
            String fusionInsight,
            String overallRisk,
            String finalImpression
    ) {}
}
