package com.factoryflow.erp.service;

import com.factoryflow.erp.dto.report.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ReportService reportService;

    // Light sage/olive tint (#CFE1B9)
    private static final byte[] SAGE_OLIVE_RGB = new byte[]{(byte) 0xCF, (byte) 0xE1, (byte) 0xB9};

    public byte[] exportReport(String reportType, String format, LocalDate startDate, LocalDate endDate) throws IOException {
        if ("csv".equalsIgnoreCase(format)) {
            return exportCsv(reportType, startDate, endDate).getBytes();
        } else {
            return exportXlsx(reportType, startDate, endDate);
        }
    }

    private byte[] exportXlsx(String reportType, LocalDate startDate, LocalDate endDate) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(capitalize(reportType));

            // Cell Styles
            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(SAGE_OLIVE_RGB, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            DataFormat df = workbook.createDataFormat();
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(df.getFormat("#,##0.00"));

            CellStyle qtyStyle = workbook.createCellStyle();
            qtyStyle.setDataFormat(df.getFormat("#,##0.000"));

            switch (reportType.toLowerCase()) {
                case "sales":
                    fillSalesXlsx(sheet, reportService.getSalesReport(startDate, endDate), headerStyle, currencyStyle);
                    break;
                case "gst-summary":
                    fillGstSummaryXlsx(sheet, reportService.getGstSummaryReport(startDate, endDate), headerStyle, currencyStyle);
                    break;
                case "purchases":
                    fillPurchasesXlsx(sheet, reportService.getPurchaseReport(startDate, endDate), headerStyle, currencyStyle, qtyStyle);
                    break;
                case "stock-summary":
                    fillStockSummaryXlsx(sheet, reportService.getStockSummaryReport(startDate, endDate), headerStyle, currencyStyle, qtyStyle);
                    break;
                case "production":
                    fillProductionXlsx(sheet, reportService.getProductionReport(startDate, endDate), headerStyle, currencyStyle, qtyStyle);
                    break;
                case "receivables":
                    fillReceivablesXlsx(sheet, reportService.getReceivablesReport(startDate, endDate), headerStyle, currencyStyle);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown report type: " + reportType);
            }

            // Auto-size columns
            for (int i = 0; i < 15; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String exportCsv(String reportType, LocalDate startDate, LocalDate endDate) {
        StringWriter sw = new StringWriter();

        switch (reportType.toLowerCase()) {
            case "sales":
                SalesRegisterReport sr = reportService.getSalesReport(startDate, endDate);
                sw.write("Invoice No,Date,Customer,GSTIN,State,Taxable,CGST,SGST,IGST,Grand Total,Status\n");
                for (SalesRegisterReport.SalesRegisterEntry e : sr.getEntries()) {
                    sw.write(String.format("%s,%s,\"%s\",%s,%s,%s,%s,%s,%s,%s,%s\n",
                            e.getInvoiceNumber(), e.getInvoiceDate(), e.getCustomerName(), e.getCustomerGstin(),
                            e.getStateName(), e.getTaxableAmount(), e.getCgstAmount(), e.getSgstAmount(),
                            e.getIgstAmount(), e.getGrandTotal(), e.getStatus()));
                }
                break;
            case "gst-summary":
                GstSummaryReport gr = reportService.getGstSummaryReport(startDate, endDate);
                sw.write("Category,Invoices,Taxable,CGST,SGST,IGST,Total Tax,Grand Total\n");
                for (GstSummaryReport.B2bVsB2cSummary b : gr.getB2bVsB2c()) {
                    sw.write(String.format("%s,%d,%s,%s,%s,%s,%s,%s\n",
                            b.getCategory(), b.getInvoiceCount(), b.getTaxableAmount(), b.getCgstAmount(),
                            b.getSgstAmount(), b.getIgstAmount(), b.getTotalTax(), b.getGrandTotal()));
                }
                break;
            case "purchases":
                PurchaseRegisterReport pr = reportService.getPurchaseReport(startDate, endDate);
                sw.write("Date,Supplier,Invoice No,Item,SKU,Unit,Qty,Unit Rate,Total Amount\n");
                for (PurchaseRegisterReport.PurchaseEntry e : pr.getEntries()) {
                    sw.write(String.format("%s,\"%s\",%s,\"%s\",%s,%s,%s,%s,%s\n",
                            e.getPurchaseDate(), e.getSupplierName(), e.getInvoiceNo(), e.getItemName(),
                            e.getSkuCode(), e.getUnit(), e.getQty(), e.getUnitRate(), e.getTotalAmount()));
                }
                break;
            case "stock-summary":
                StockSummaryReport st = reportService.getStockSummaryReport(startDate, endDate);
                sw.write("SKU,Item Name,Type,Unit,Opening,Receipts,Issues & Sales,Closing,Rate,Value\n");
                for (StockSummaryReport.StockSummaryItem i : st.getItems()) {
                    sw.write(String.format("%s,\"%s\",%s,%s,%s,%s,%s,%s,%s,%s\n",
                            i.getSkuCode(), i.getItemName(), i.getItemType(), i.getUnit(),
                            i.getOpeningBalance(), i.getReceipts(), i.getIssuesAndSales(), i.getClosingBalance(),
                            i.getStandardRate(), i.getClosingStockValue()));
                }
                break;
            case "production":
                ProductionReport pdr = reportService.getProductionReport(startDate, endDate);
                sw.write("Run Code,Date,Product,Planned,Output,Yield Variance %,Damaged Pouches,Cartons,Labour,Overhead,Unit Cost\n");
                for (ProductionReport.ProductionRunDetail r : pdr.getRuns()) {
                    sw.write(String.format("%s,%s,\"%s\",%s,%d,%s,%d,%d,%s,%s,%s\n",
                            r.getRunCode(), r.getRunDate(), r.getProductName(), r.getBatchesPlanned(),
                            r.getActualGoodPackets(), r.getYieldVariancePct(), r.getDamagedPouches(),
                            r.getCartonsUsed(), r.getLabourCost(), r.getOverheadCost(), r.getCalculatedUnitCost()));
                }
                break;
            case "receivables":
                ReceivablesAgeingReport rar = reportService.getReceivablesReport(startDate, endDate);
                sw.write("Customer,GSTIN,State,Total Invoiced,Total Paid,Balance Due,0-30 Days,31-60 Days,60+ Days\n");
                for (ReceivablesAgeingReport.CustomerAgeing c : rar.getCustomerAgeing()) {
                    sw.write(String.format("\"%s\",%s,%s,%s,%s,%s,%s,%s,%s\n",
                            c.getCustomerName(), c.getGstin(), c.getStateName(), c.getTotalInvoiced(),
                            c.getTotalPaid(), c.getBalanceDue(), c.getBucket0to30(), c.getBucket31to60(), c.getBucket60Plus()));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown report type: " + reportType);
        }

        return sw.toString();
    }

    private void fillSalesXlsx(XSSFSheet sheet, SalesRegisterReport r, CellStyle hStyle, CellStyle cStyle) {
        String[] headers = {"Invoice No", "Date", "Customer", "GSTIN", "State", "Taxable Amount", "CGST", "SGST", "IGST", "Grand Total", "Status"};
        createHeaderRow(sheet, headers, hStyle);

        int rowIdx = 1;
        for (SalesRegisterReport.SalesRegisterEntry e : r.getEntries()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getInvoiceNumber());
            row.createCell(1).setCellValue(e.getInvoiceDate().toString());
            row.createCell(2).setCellValue(e.getCustomerName());
            row.createCell(3).setCellValue(e.getCustomerGstin());
            row.createCell(4).setCellValue(e.getStateName());
            setNumCell(row, 5, e.getTaxableAmount(), cStyle);
            setNumCell(row, 6, e.getCgstAmount(), cStyle);
            setNumCell(row, 7, e.getSgstAmount(), cStyle);
            setNumCell(row, 8, e.getIgstAmount(), cStyle);
            setNumCell(row, 9, e.getGrandTotal(), cStyle);
            row.createCell(10).setCellValue(e.getStatus());
        }
    }

    private void fillGstSummaryXlsx(XSSFSheet sheet, GstSummaryReport r, CellStyle hStyle, CellStyle cStyle) {
        String[] headers = {"Category", "Invoice Count", "Taxable Value", "CGST Amount", "SGST Amount", "IGST Amount", "Total Tax", "Grand Total"};
        createHeaderRow(sheet, headers, hStyle);

        int rowIdx = 1;
        for (GstSummaryReport.B2bVsB2cSummary b : r.getB2bVsB2c()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(b.getCategory());
            row.createCell(1).setCellValue(b.getInvoiceCount());
            setNumCell(row, 2, b.getTaxableAmount(), cStyle);
            setNumCell(row, 3, b.getCgstAmount(), cStyle);
            setNumCell(row, 4, b.getSgstAmount(), cStyle);
            setNumCell(row, 5, b.getIgstAmount(), cStyle);
            setNumCell(row, 6, b.getTotalTax(), cStyle);
            setNumCell(row, 7, b.getGrandTotal(), cStyle);
        }
    }

    private void fillPurchasesXlsx(XSSFSheet sheet, PurchaseRegisterReport r, CellStyle hStyle, CellStyle cStyle, CellStyle qStyle) {
        String[] headers = {"Date", "Supplier", "Invoice No", "Item Name", "SKU", "Unit", "Qty", "Unit Rate", "Total Amount"};
        createHeaderRow(sheet, headers, hStyle);

        int rowIdx = 1;
        for (PurchaseRegisterReport.PurchaseEntry e : r.getEntries()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getPurchaseDate().toString());
            row.createCell(1).setCellValue(e.getSupplierName());
            row.createCell(2).setCellValue(e.getInvoiceNo());
            row.createCell(3).setCellValue(e.getItemName());
            row.createCell(4).setCellValue(e.getSkuCode());
            row.createCell(5).setCellValue(e.getUnit().name());
            setNumCell(row, 6, e.getQty(), qStyle);
            setNumCell(row, 7, e.getUnitRate(), cStyle);
            setNumCell(row, 8, e.getTotalAmount(), cStyle);
        }
    }

    private void fillStockSummaryXlsx(XSSFSheet sheet, StockSummaryReport r, CellStyle hStyle, CellStyle cStyle, CellStyle qStyle) {
        String[] headers = {"SKU", "Item Name", "Type", "Unit", "Opening Balance", "Receipts (+)", "Issues/Sales (-)", "Closing Balance", "Std Rate", "Stock Value"};
        createHeaderRow(sheet, headers, hStyle);

        int rowIdx = 1;
        for (StockSummaryReport.StockSummaryItem i : r.getItems()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(i.getSkuCode());
            row.createCell(1).setCellValue(i.getItemName());
            row.createCell(2).setCellValue(i.getItemType().name());
            row.createCell(3).setCellValue(i.getUnit().name());
            setNumCell(row, 4, i.getOpeningBalance(), qStyle);
            setNumCell(row, 5, i.getReceipts(), qStyle);
            setNumCell(row, 6, i.getIssuesAndSales(), qStyle);
            setNumCell(row, 7, i.getClosingBalance(), qStyle);
            setNumCell(row, 8, i.getStandardRate(), cStyle);
            setNumCell(row, 9, i.getClosingStockValue(), cStyle);
        }
    }

    private void fillProductionXlsx(XSSFSheet sheet, ProductionReport r, CellStyle hStyle, CellStyle cStyle, CellStyle qStyle) {
        String[] headers = {"Run Code", "Date", "Product", "Batches", "Output Pkts", "Yield Var %", "Damaged", "Cartons", "Labour", "Overhead", "Cost/Packet"};
        createHeaderRow(sheet, headers, hStyle);

        int rowIdx = 1;
        for (ProductionReport.ProductionRunDetail d : r.getRuns()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(d.getRunCode());
            row.createCell(1).setCellValue(d.getRunDate().toString());
            row.createCell(2).setCellValue(d.getProductName());
            setNumCell(row, 3, d.getBatchesPlanned(), cStyle);
            row.createCell(4).setCellValue(d.getActualGoodPackets());
            setNumCell(row, 5, d.getYieldVariancePct(), cStyle);
            row.createCell(6).setCellValue(d.getDamagedPouches());
            row.createCell(7).setCellValue(d.getCartonsUsed());
            setNumCell(row, 8, d.getLabourCost(), cStyle);
            setNumCell(row, 9, d.getOverheadCost(), cStyle);
            setNumCell(row, 10, d.getCalculatedUnitCost(), cStyle);
        }
    }

    private void fillReceivablesXlsx(XSSFSheet sheet, ReceivablesAgeingReport r, CellStyle hStyle, CellStyle cStyle) {
        String[] headers = {"Customer", "GSTIN", "State", "Total Invoiced", "Total Paid", "Balance Due", "0-30 Days", "31-60 Days", "60+ Days"};
        createHeaderRow(sheet, headers, hStyle);

        int rowIdx = 1;
        for (ReceivablesAgeingReport.CustomerAgeing c : r.getCustomerAgeing()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(c.getCustomerName());
            row.createCell(1).setCellValue(c.getGstin());
            row.createCell(2).setCellValue(c.getStateName());
            setNumCell(row, 3, c.getTotalInvoiced(), cStyle);
            setNumCell(row, 4, c.getTotalPaid(), cStyle);
            setNumCell(row, 5, c.getBalanceDue(), cStyle);
            setNumCell(row, 6, c.getBucket0to30(), cStyle);
            setNumCell(row, 7, c.getBucket31to60(), cStyle);
            setNumCell(row, 8, c.getBucket60Plus(), cStyle);
        }
    }

    private void createHeaderRow(XSSFSheet sheet, String[] headers, CellStyle hStyle) {
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(hStyle);
        }
    }

    private void setNumCell(Row row, int col, BigDecimal val, CellStyle style) {
        Cell cell = row.createCell(col);
        if (val != null) {
            cell.setCellValue(val.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
