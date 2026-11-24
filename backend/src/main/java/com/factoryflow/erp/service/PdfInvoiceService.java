package com.factoryflow.erp.service;

import com.factoryflow.erp.domain.entity.Customer;
import com.factoryflow.erp.domain.entity.Invoice;
import com.factoryflow.erp.domain.entity.SalesOrder;
import com.factoryflow.erp.domain.entity.SalesOrderLine;
import com.factoryflow.erp.exception.ResourceNotFoundException;
import com.factoryflow.erp.repository.InvoiceRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfInvoiceService {

    private final InvoiceRepository invoiceRepository;

    private static final String FACTORY_NAME = "FACTORY FLOW FOOD PRODUCTS PVT LTD";
    private static final String FACTORY_ADDRESS = "Plot 42, SIDCO Industrial Estate, Thuvakudi, Trichy - 620015";
    private static final String FACTORY_GSTIN = "33AABCF9999K1Z5";
    private static final String FACTORY_STATE = "Tamil Nadu (Code: 33)";

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

            // 1. Header
            Paragraph title = new Paragraph("TAX INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph factoryInfo = new Paragraph(
                    FACTORY_NAME + "\n" +
                    FACTORY_ADDRESS + "\n" +
                    "GSTIN: " + FACTORY_GSTIN + " | State: " + FACTORY_STATE,
                    subHeaderFont
            );
            factoryInfo.setAlignment(Element.ALIGN_CENTER);
            factoryInfo.setSpacingAfter(15);
            document.add(factoryInfo);

            // 2. Invoice Details & Customer Info
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{1f, 1f});

            Customer customer = invoice.getCustomer();
            SalesOrder order = invoice.getOrder();

            PdfPCell leftMeta = new PdfPCell();
            leftMeta.setBorder(Rectangle.BOX);
            leftMeta.setPadding(6);
            leftMeta.addElement(new Paragraph("Billed To (Customer):", boldFont));
            leftMeta.addElement(new Paragraph(customer.getName(), normalFont));
            leftMeta.addElement(new Paragraph("GSTIN: " + (customer.getGstin() != null ? customer.getGstin() : "URP"), normalFont));
            leftMeta.addElement(new Paragraph("State: " + customer.getStateName(), normalFont));
            if (customer.getPhone() != null) {
                leftMeta.addElement(new Paragraph("Phone: " + customer.getPhone(), normalFont));
            }

            PdfPCell rightMeta = new PdfPCell();
            rightMeta.setBorder(Rectangle.BOX);
            rightMeta.setPadding(6);
            rightMeta.addElement(new Paragraph("Invoice No: " + invoice.getInvoiceNumber(), boldFont));
            rightMeta.addElement(new Paragraph("Invoice Date: " + invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")), normalFont));
            rightMeta.addElement(new Paragraph("Order Ref: " + (order != null ? order.getOrderNumber() : "N/A"), normalFont));
            rightMeta.addElement(new Paragraph("Place of Supply: " + customer.getStateName(), normalFont));
            rightMeta.addElement(new Paragraph("Status: " + invoice.getStatus().name(), boldFont));

            metaTable.addCell(leftMeta);
            metaTable.addCell(rightMeta);
            metaTable.setSpacingAfter(15);
            document.add(metaTable);

            // 3. Line Items Table
            boolean isIntraState = invoice.getCgstAmount().compareTo(BigDecimal.ZERO) > 0 ||
                                  invoice.getSgstAmount().compareTo(BigDecimal.ZERO) > 0;

            PdfPTable itemTable = new PdfPTable(isIntraState ? 8 : 7);
            itemTable.setWidthPercentage(100);
            if (isIntraState) {
                itemTable.setWidths(new float[]{0.6f, 2.5f, 1f, 0.8f, 1f, 1.2f, 1f, 1f});
            } else {
                itemTable.setWidths(new float[]{0.6f, 3f, 1.2f, 0.9f, 1.2f, 1.4f, 1.2f});
            }

            // Headers
            addHeaderCell(itemTable, "#", boldFont);
            addHeaderCell(itemTable, "Description of Goods", boldFont);
            addHeaderCell(itemTable, "HSN Code", boldFont);
            addHeaderCell(itemTable, "Qty", boldFont);
            addHeaderCell(itemTable, "Rate (Rs)", boldFont);
            addHeaderCell(itemTable, "Taxable (Rs)", boldFont);
            if (isIntraState) {
                addHeaderCell(itemTable, "CGST (2.5%)", boldFont);
                addHeaderCell(itemTable, "SGST (2.5%)", boldFont);
            } else {
                addHeaderCell(itemTable, "IGST (5%)", boldFont);
            }

            int index = 1;
            if (order != null && order.getLines() != null) {
                for (SalesOrderLine line : order.getLines()) {
                    BigDecimal lineQty = BigDecimal.valueOf(line.getQty());
                    BigDecimal lineTaxable = lineQty.multiply(line.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);

                    addBodyCell(itemTable, String.valueOf(index++), normalFont, Element.ALIGN_CENTER);
                    addBodyCell(itemTable, line.getItem().getName() + " (" + line.getItem().getSkuCode() + ")", normalFont, Element.ALIGN_LEFT);
                    addBodyCell(itemTable, line.getItem().getHsnCode() != null ? line.getItem().getHsnCode() : "21069099", normalFont, Element.ALIGN_CENTER);
                    addBodyCell(itemTable, String.valueOf(line.getQty()), normalFont, Element.ALIGN_RIGHT);
                    addBodyCell(itemTable, line.getUnitPrice().toString(), normalFont, Element.ALIGN_RIGHT);
                    addBodyCell(itemTable, lineTaxable.toString(), normalFont, Element.ALIGN_RIGHT);

                    if (isIntraState) {
                        BigDecimal halfTax = lineTaxable.multiply(new BigDecimal("0.025")).setScale(2, RoundingMode.HALF_UP);
                        addBodyCell(itemTable, halfTax.toString(), normalFont, Element.ALIGN_RIGHT);
                        addBodyCell(itemTable, halfTax.toString(), normalFont, Element.ALIGN_RIGHT);
                    } else {
                        BigDecimal igst = lineTaxable.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
                        addBodyCell(itemTable, igst.toString(), normalFont, Element.ALIGN_RIGHT);
                    }
                }
            }

            // Subtotals
            int colspan = 5;
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Totals:", boldFont));
            totalLabelCell.setColspan(colspan);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            itemTable.addCell(totalLabelCell);

            addBodyCell(itemTable, invoice.getTaxableAmount().toString(), boldFont, Element.ALIGN_RIGHT);
            if (isIntraState) {
                addBodyCell(itemTable, invoice.getCgstAmount().toString(), boldFont, Element.ALIGN_RIGHT);
                addBodyCell(itemTable, invoice.getSgstAmount().toString(), boldFont, Element.ALIGN_RIGHT);
            } else {
                addBodyCell(itemTable, invoice.getIgstAmount().toString(), boldFont, Element.ALIGN_RIGHT);
            }

            itemTable.setSpacingAfter(15);
            document.add(itemTable);

            // 4. Grand Total & Words
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{2f, 1f});

            PdfPCell wordsCell = new PdfPCell();
            wordsCell.setBorder(Rectangle.BOX);
            wordsCell.setPadding(6);
            wordsCell.addElement(new Paragraph("Amount Chargeable (in words):", boldFont));
            wordsCell.addElement(new Paragraph("INR " + convertToIndianWords(invoice.getGrandTotal().longValue()) + " Only", normalFont));

            PdfPCell numCell = new PdfPCell();
            numCell.setBorder(Rectangle.BOX);
            numCell.setPadding(6);
            numCell.addElement(new Paragraph("Grand Total: Rs " + invoice.getGrandTotal().toString(), boldFont));
            numCell.addElement(new Paragraph("Taxable: Rs " + invoice.getTaxableAmount().toString(), normalFont));
            if (isIntraState) {
                numCell.addElement(new Paragraph("CGST: Rs " + invoice.getCgstAmount().toString(), normalFont));
                numCell.addElement(new Paragraph("SGST: Rs " + invoice.getSgstAmount().toString(), normalFont));
            } else {
                numCell.addElement(new Paragraph("IGST: Rs " + invoice.getIgstAmount().toString(), normalFont));
            }

            summaryTable.addCell(wordsCell);
            summaryTable.addCell(numCell);
            summaryTable.setSpacingAfter(25);
            document.add(summaryTable);

            // 5. Signatory Footer
            Paragraph footer = new Paragraph(
                    "For " + FACTORY_NAME + "\n\n\n\n" +
                    "Authorized Signatory",
                    boldFont
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF invoice", e);
        }

        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }

    /**
     * Converts a number to Indian currency words format.
     */
    private String convertToIndianWords(long n) {
        if (n == 0) return "Zero";

        String[] units = {
                "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
                "Seventeen", "Eighteen", "Nineteen"
        };

        String[] tens = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        };

        StringBuilder words = new StringBuilder();

        if (n >= 10000000) {
            words.append(convertToIndianWords(n / 10000000)).append(" Crore ");
            n %= 10000000;
        }
        if (n >= 100000) {
            words.append(convertToIndianWords(n / 100000)).append(" Lakh ");
            n %= 100000;
        }
        if (n >= 1000) {
            words.append(convertToIndianWords(n / 1000)).append(" Thousand ");
            n %= 1000;
        }
        if (n >= 100) {
            words.append(convertToIndianWords(n / 100)).append(" Hundred ");
            n %= 100;
        }
        if (n > 0) {
            if (n < 20) {
                words.append(units[(int) n]);
            } else {
                words.append(tens[(int) (n / 10)]);
                if (n % 10 > 0) {
                    words.append(" ").append(units[(int) (n % 10)]);
                }
            }
        }

        return words.toString().trim();
    }
}
