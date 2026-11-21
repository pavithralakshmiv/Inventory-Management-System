package com.inventory.service;

import com.inventory.dto.LineResponse;
import com.inventory.dto.SaleResponse;
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
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    private final SaleService saleService;
    private final SettingService settingService;

    public InvoiceService(SaleService saleService, SettingService settingService) {
        this.saleService = saleService;
        this.settingService = settingService;
    }

    public byte[] invoicePdf(Long saleId) {
        SaleResponse sale = saleService.get(saleId);
        String currency = pdfCurrency(settingService.currency());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();
            Font big = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph name = new Paragraph(settingService.get("business_name", "Inventory Management System"), big);
            name.setAlignment(Element.ALIGN_CENTER);
            document.add(name);
            Paragraph contact = new Paragraph(settingService.get("business_address", "") + "  |  "
                    + settingService.get("business_phone", "") + "  |  " + settingService.get("business_email", ""), normal);
            contact.setAlignment(Element.ALIGN_CENTER);
            contact.setSpacingAfter(12);
            document.add(contact);

            Paragraph heading = new Paragraph("TAX INVOICE" + ("Cancelled".equals(sale.status()) ? "  (CANCELLED)" : ""), bold);
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingAfter(10);
            document.add(heading);

            document.add(new Paragraph("Invoice No: " + sale.invoiceNumber(), normal));
            document.add(new Paragraph("Date: " + sale.saleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normal));
            document.add(new Paragraph("Customer: " + sale.customerName(), normal));
            Paragraph payment = new Paragraph("Payment Method: " + sale.paymentMethod(), normal);
            payment.setSpacingAfter(10);
            document.add(payment);

            PdfPTable table = new PdfPTable(new float[]{1, 4, 1.4f, 2, 2});
            table.setWidthPercentage(100);
            for (String head : new String[]{"#", "Product", "Qty", "Price", "Total"}) {
                PdfPCell cell = new PdfPCell(new Phrase(head, bold));
                cell.setBackgroundColor(new Color(255, 201, 40));
                cell.setPadding(5);
                table.addCell(cell);
            }
            int index = 1;
            for (LineResponse line : sale.items()) {
                table.addCell(cell(String.valueOf(index++), normal, Element.ALIGN_LEFT));
                table.addCell(cell(line.productName(), normal, Element.ALIGN_LEFT));
                table.addCell(cell(String.valueOf(line.quantity()), normal, Element.ALIGN_RIGHT));
                table.addCell(cell(money(currency, line.unitPrice()), normal, Element.ALIGN_RIGHT));
                table.addCell(cell(money(currency, line.lineTotal()), normal, Element.ALIGN_RIGHT));
            }
            document.add(table);

            PdfPTable totals = new PdfPTable(new float[]{6, 2});
            totals.setWidthPercentage(100);
            totals.setSpacingBefore(8);
            addTotal(totals, "Subtotal", money(currency, sale.subtotal()), normal);
            addTotal(totals, "Discount (" + sale.discountPercent().stripTrailingZeros().toPlainString() + "%)",
                    "- " + money(currency, sale.discountAmount()), normal);
            addTotal(totals, "Grand Total", money(currency, sale.total()), bold);
            document.add(totals);

            Paragraph thanks = new Paragraph("Thank you for your business!", normal);
            thanks.setAlignment(Element.ALIGN_CENTER);
            thanks.setSpacingBefore(25);
            document.add(thanks);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new BusinessException("Could not create invoice: " + ex.getMessage());
        }
    }

    private PdfPCell cell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private void addTotal(PdfPTable table, String label, String value, Font font) {
        PdfPCell left = new PdfPCell(new Phrase(label, font));
        left.setBorder(0);
        left.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell right = new PdfPCell(new Phrase(value, font));
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(left);
        table.addCell(right);
    }

    private String money(String currency, BigDecimal value) {
        return currency + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String pdfCurrency(String symbol) {
        if ("₹".equals(symbol)) {
            return "Rs. ";
        }
        return symbol != null && symbol.chars().allMatch(c -> c < 128) ? symbol : "";
    }
}
