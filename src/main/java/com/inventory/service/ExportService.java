package com.inventory.service;

import com.inventory.dto.ReportResult;
import com.inventory.exception.BusinessException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private final SettingService settingService;

    public ExportService(SettingService settingService) {
        this.settingService = settingService;
    }

    public byte[] toPdf(ReportResult report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            boolean wide = report.columns().size() > 6;
            Document document = new Document(wide ? PageSize.A4.rotate() : PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, out);
            document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Paragraph business = new Paragraph(settingService.get("business_name", "Inventory Management System"), titleFont);
            business.setAlignment(Element.ALIGN_CENTER);
            document.add(business);
            Paragraph title = new Paragraph(report.title(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph generated = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), smallFont);
            generated.setAlignment(Element.ALIGN_CENTER);
            generated.setSpacingAfter(10);
            document.add(generated);

            PdfPTable table = new PdfPTable(report.columns().size());
            table.setWidthPercentage(100);
            for (String column : report.columns()) {
                PdfPCell cell = new PdfPCell(new Phrase(column, headFont));
                cell.setBackgroundColor(new Color(255, 201, 40));
                cell.setPadding(4);
                table.addCell(cell);
            }
            for (List<Object> row : report.rows()) {
                for (Object value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(text(value), smallFont));
                    cell.setPadding(3);
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new BusinessException("Could not create PDF: " + ex.getMessage());
        }
    }

    public byte[] toExcel(ReportResult report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(report.title());
            CellStyle headStyle = workbook.createCellStyle();
            headStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font bold = workbook.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);
            Row head = sheet.createRow(0);
            for (int i = 0; i < report.columns().size(); i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(report.columns().get(i));
                cell.setCellStyle(headStyle);
            }
            int rowIndex = 1;
            for (List<Object> row : report.rows()) {
                Row excelRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < row.size(); i++) {
                    Cell cell = excelRow.createCell(i);
                    Object value = row.get(i);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(text(value));
                    }
                }
            }
            for (int i = 0; i < report.columns().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Could not create Excel file: " + ex.getMessage());
        }
    }

    private String text(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return String.valueOf(value);
    }
}
