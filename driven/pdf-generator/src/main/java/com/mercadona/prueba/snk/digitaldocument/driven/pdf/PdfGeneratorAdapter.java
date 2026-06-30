package com.mercadona.prueba.snk.digitaldocument.driven.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mercadona.prueba.snk.digitaldocument.application.model.PdfGenerationRequest;
import com.mercadona.prueba.snk.digitaldocument.application.model.PdfResult;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.PdfGenerator;
import com.mercadona.prueba.snk.digitaldocument.domain.AiCertificationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PdfGeneratorAdapter implements PdfGenerator {

  private static final String CONTENT_TYPE = "application/pdf";
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private static final Color COLOR_MERCADONA = new Color(0, 95, 60);
  private static final Color COLOR_HEADER_BG = new Color(0, 95, 60);
  private static final Color COLOR_ROW_ALT = new Color(240, 248, 244);

  @Override
  public PdfResult generate(PdfGenerationRequest request) {
    log.info("event=PDF_GENERATE documentId={}", request.getDocumentId());

    byte[] content = buildPdf(request);
    String checksum = sha256(content);

    log.info("event=PDF_GENERATED documentId={} size={} checksum={}", request.getDocumentId(), content.length, checksum);
    return PdfResult.builder()
        .content(content)
        .checksum(checksum)
        .contentType(CONTENT_TYPE)
        .build();
  }

  private byte[] buildPdf(PdfGenerationRequest req) {
    var out = new ByteArrayOutputStream();
    var document = new Document(PageSize.A4, 50, 50, 60, 60);

    try {
      var writer = PdfWriter.getInstance(document, out);

      // Fix creation date for deterministic output
      var cal = GregorianCalendar.from(req.getGenerationDate().toZonedDateTime());
      writer.getInfo().put(com.lowagie.text.pdf.PdfName.CREATIONDATE,
          new com.lowagie.text.pdf.PdfDate(cal));
      writer.getInfo().put(com.lowagie.text.pdf.PdfName.MODDATE,
          new com.lowagie.text.pdf.PdfDate(cal));

      document.open();
      addHeader(document, req);
      addDocumentInfo(document, req);
      addEmployeeSection(document, req);
      addCertificationSection(document, req);
      addFooter(document, req);
      document.close();

    } catch (Exception e) {
      throw new IllegalStateException("Error generating PDF for document " + req.getDocumentId(), e);
    }
    return out.toByteArray();
  }

  private void addHeader(Document doc, PdfGenerationRequest req) throws Exception {
    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_MERCADONA);
    Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);

    var title = new Paragraph("MERCADONA", titleFont);
    title.setAlignment(Element.ALIGN_CENTER);
    doc.add(title);

    var subtitle = new Paragraph("Certificación de Uso de Herramientas de IA Generativa", subtitleFont);
    subtitle.setAlignment(Element.ALIGN_CENTER);
    subtitle.setSpacingBefore(4);
    doc.add(subtitle);

    addSeparator(doc);
  }

  private void addDocumentInfo(Document doc, PdfGenerationRequest req) throws Exception {
    Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

    var table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setWidths(new float[]{30, 70});
    table.setSpacingBefore(10);

    addTableRow(table, "ID Documento:", req.getDocumentId().toString(), labelFont, valueFont, false);
    addTableRow(table, "Fecha de emisión:",
        req.getGenerationDate().format(DATETIME_FMT), labelFont, valueFont, true);
    doc.add(table);
  }

  private void addEmployeeSection(Document doc, PdfGenerationRequest req) throws Exception {
    addSectionTitle(doc, "DATOS DEL EMPLEADO");

    Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

    var emp = req.getEmployeeData();
    var table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setWidths(new float[]{30, 70});

    boolean alt = false;
    addTableRow(table, "Nombre completo:", safe(emp != null ? emp.getFullName() : null), labelFont, valueFont, alt = !alt);
    addTableRow(table, "ID empleado:", req.getEmployeeId(), labelFont, valueFont, alt = !alt);
    addTableRow(table, "Grupo gestionado:", req.getManagedGroupId(), labelFont, valueFont, alt = !alt);
    if (emp != null) {
      addTableRow(table, "Función:", safe(emp.getJobFunction()), labelFont, valueFont, alt = !alt);
      addTableRow(table, "Departamento:", safe(emp.getDepartment()), labelFont, valueFont, alt = !alt);
      addTableRow(table, "Email:", safe(emp.getEmail()), labelFont, valueFont, alt = !alt);
      addTableRow(table, "Ubicación:", safe(emp.getLocation()), labelFont, valueFont, alt = !alt);
    }
    doc.add(table);
  }

  private void addCertificationSection(Document doc, PdfGenerationRequest req) throws Exception {
    addSectionTitle(doc, "CERTIFICACIÓN IA");

    Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

    AiCertificationData cert = Optional.ofNullable(req.getEmployeeData())
        .map(e -> e.getCertification())
        .orElse(null);

    var table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setWidths(new float[]{30, 70});

    boolean alt = false;
    if (cert != null) {
      addTableRow(table, "Estado:", safe(cert.getStatus()), labelFont, valueFont, alt = !alt);
      addTableRow(table, "Certificación vigente:", cert.isValid() ? "Sí" : "No", labelFont, valueFont, alt = !alt);
      addTableRow(table, "Nivel:", safe(cert.getLevel()), labelFont, valueFont, alt = !alt);
      addTableRow(table, "Herramientas aprobadas:", formatList(cert.getApprovedTools()), labelFont, valueFont, alt = !alt);
      addTableRow(table, "Válida desde:", cert.getStartDate() != null ? cert.getStartDate().format(DATE_FMT) : "-", labelFont, valueFont, alt = !alt);
      addTableRow(table, "Válida hasta:", cert.getExpirationDate() != null ? cert.getExpirationDate().format(DATE_FMT) : "-", labelFont, valueFont, alt = !alt);
      addTableRow(table, "Emitida por:", safe(cert.getIssuedBy()), labelFont, valueFont, alt = !alt);
      addTableRow(table, "ID Certificación:", safe(cert.getCertificationId()), labelFont, valueFont, alt = !alt);
    } else {
      addTableRow(table, "Estado:", "Sin datos de certificación", labelFont, valueFont, false);
    }
    doc.add(table);
  }

  private void addFooter(Document doc, PdfGenerationRequest req) throws Exception {
    Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
    var footer = new Paragraph(
        "Documento generado automáticamente. ID: " + req.getDocumentId(), footerFont);
    footer.setAlignment(Element.ALIGN_CENTER);
    footer.setSpacingBefore(20);
    doc.add(footer);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void addSeparator(Document doc) throws Exception {
    var sep = new Paragraph(" ");
    sep.setSpacingAfter(4);
    doc.add(sep);
    // Thin green line
    var line = new com.lowagie.text.pdf.draw.LineSeparator(1f, 100f, COLOR_MERCADONA,
        Element.ALIGN_CENTER, -2);
    doc.add(new com.lowagie.text.Chunk(line));
  }

  private void addSectionTitle(Document doc, String title) throws Exception {
    Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    var cell = new PdfPCell(new Phrase(title, sectionFont));
    cell.setBackgroundColor(COLOR_HEADER_BG);
    cell.setPadding(6);
    cell.setBorder(Rectangle.NO_BORDER);

    var table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingBefore(14);
    table.setSpacingAfter(4);
    table.addCell(cell);
    doc.add(table);
  }

  private void addTableRow(PdfPTable table, String label, String value,
      Font labelFont, Font valueFont, boolean alternate) {
    Color bg = alternate ? COLOR_ROW_ALT : Color.WHITE;

    PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
    labelCell.setBackgroundColor(bg);
    labelCell.setPadding(4);
    labelCell.setBorderColor(Color.LIGHT_GRAY);

    PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
    valueCell.setBackgroundColor(bg);
    valueCell.setPadding(4);
    valueCell.setBorderColor(Color.LIGHT_GRAY);

    table.addCell(labelCell);
    table.addCell(valueCell);
  }

  private String safe(String s) {
    return s != null ? s : "-";
  }

  private String formatList(List<String> list) {
    if (list == null || list.isEmpty()) return "-";
    return String.join(", ", list);
  }

  private String sha256(byte[] content) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
