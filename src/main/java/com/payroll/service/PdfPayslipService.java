package com.payroll.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.payroll.model.PayrollRecord;
import com.payroll.utils.NumberToWordsConverter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfPayslipService {

    public byte[] generatePayslipPdf(PayrollRecord record) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            Color primaryNavy = new Color(30, 58, 138);     // #1e3a8a
            Color darkSlate = new Color(15, 23, 42);        // #0f172a
            Color lightGray = new Color(241, 245, 249);     // #f1f5f9
            Color borderGray = new Color(203, 213, 225);    // #cbd5e1
            Color emeraldGreen = new Color(16, 185, 129);   // #10b981

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryNavy);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font sectionTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryNavy);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, darkSlate);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, darkSlate);
            Font netPayFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryNavy);
            Font netPayLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);

            // 1. Company Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{65, 35});
            headerTable.setSpacingAfter(15);

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);
            leftHeader.addElement(new Paragraph("NEXUS ENTERPRISE HRMS", titleFont));
            leftHeader.addElement(new Paragraph("Corporate HR & Payroll Division\n100 Technology Boulevard, Innovation Park", subtitleFont));
            headerTable.addCell(leftHeader);

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph pSlip = new Paragraph("SALARY PAYSLIP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, primaryNavy));
            pSlip.setAlignment(Element.ALIGN_RIGHT);
            Paragraph pRef = new Paragraph("Ref: " + record.getPayrollNumber() + "\nPeriod: " +
                    getMonthName(record.getMonth()) + " " + record.getYear() + "\nStatus: " + record.getPaymentStatus(), subtitleFont);
            pRef.setAlignment(Element.ALIGN_RIGHT);
            rightHeader.addElement(pSlip);
            rightHeader.addElement(pRef);
            headerTable.addCell(rightHeader);

            document.add(headerTable);

            // Divider Line
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            PdfPCell dCell = new PdfPCell();
            dCell.setFixedHeight(2);
            dCell.setBackgroundColor(primaryNavy);
            dCell.setBorder(Rectangle.NO_BORDER);
            divider.addCell(dCell);
            divider.setSpacingAfter(12);
            document.add(divider);

            // 2. Employee Info Table
            PdfPTable empTable = new PdfPTable(4);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{22, 28, 22, 28});
            empTable.setSpacingAfter(15);

            addInfoCell(empTable, "Employee Code:", record.getEmployee().getEmployeeCode(), boldFont, regularFont, lightGray);
            addInfoCell(empTable, "Employee Name:", record.getEmployee().getFullName(), boldFont, regularFont, lightGray);
            addInfoCell(empTable, "Department:", (record.getEmployee().getDepartment() != null ? record.getEmployee().getDepartment().getName() : "N/A"), boldFont, regularFont, lightGray);
            addInfoCell(empTable, "Designation:", record.getEmployee().getDesignation(), boldFont, regularFont, lightGray);
            addInfoCell(empTable, "Joining Date:", record.getEmployee().getJoiningDate().toString(), boldFont, regularFont, lightGray);
            addInfoCell(empTable, "Payment Method:", record.getPaymentMethod(), boldFont, regularFont, lightGray);
            addInfoCell(empTable, "Payment Date:", (record.getPaymentDate() != null ? record.getPaymentDate().toString() : "Pending"), boldFont, regularFont, lightGray);
            addInfoCell(empTable, "Generated On:", record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), boldFont, regularFont, lightGray);

            document.add(empTable);

            // 3. Earnings & Deductions Table (Side by Side)
            PdfPTable salaryTable = new PdfPTable(4);
            salaryTable.setWidthPercentage(100);
            salaryTable.setWidths(new float[]{32, 18, 32, 18});
            salaryTable.setSpacingAfter(15);

            // Table Headers
            addHeaderCell(salaryTable, "EARNINGS", primaryNavy);
            addHeaderCell(salaryTable, "AMOUNT (INR)", primaryNavy);
            addHeaderCell(salaryTable, "DEDUCTIONS", primaryNavy);
            addHeaderCell(salaryTable, "AMOUNT (INR)", primaryNavy);

            // Rows
            addSalaryRow(salaryTable, "Basic Salary", record.getBasicSalary(), "Provident Fund (PF)", record.getPfDeduction(), regularFont, borderGray);
            addSalaryRow(salaryTable, "House Rent Allowance (HRA)", record.getHra(), "Tax Deducted at Source (TDS)", record.getTaxDeduction(), regularFont, borderGray);
            addSalaryRow(salaryTable, "Dearness Allowance (DA)", record.getDa(), "Other Deductions", record.getOtherDeductions(), regularFont, borderGray);
            addSalaryRow(salaryTable, "Overtime Pay", record.getOvertimePay(), "", null, regularFont, borderGray);
            addSalaryRow(salaryTable, "Performance Bonus", record.getBonus(), "", null, regularFont, borderGray);
            addSalaryRow(salaryTable, "Special Allowances", record.getOtherAllowances(), "", null, regularFont, borderGray);

            // Subtotal row
            PdfPCell cGrossL = new PdfPCell(new Phrase("Total Gross Earnings", boldFont));
            cGrossL.setBackgroundColor(lightGray);
            salaryTable.addCell(cGrossL);

            PdfPCell cGrossR = new PdfPCell(new Phrase("INR " + formatCurrency(record.getGrossSalary()), boldFont));
            cGrossR.setBackgroundColor(lightGray);
            cGrossR.setHorizontalAlignment(Element.ALIGN_RIGHT);
            salaryTable.addCell(cGrossR);

            PdfPCell cDedL = new PdfPCell(new Phrase("Total Deductions", boldFont));
            cDedL.setBackgroundColor(lightGray);
            salaryTable.addCell(cDedL);

            PdfPCell cDedR = new PdfPCell(new Phrase("INR " + formatCurrency(record.getTotalDeductions()), boldFont));
            cDedR.setBackgroundColor(lightGray);
            cDedR.setHorizontalAlignment(Element.ALIGN_RIGHT);
            salaryTable.addCell(cDedR);

            document.add(salaryTable);

            // 4. Net Pay Card
            PdfPTable netTable = new PdfPTable(2);
            netTable.setWidthPercentage(100);
            netTable.setWidths(new float[]{60, 40});
            netTable.setSpacingAfter(25);

            PdfPCell netWordsCell = new PdfPCell();
            netWordsCell.setBackgroundColor(lightGray);
            netWordsCell.setPadding(10);
            netWordsCell.addElement(new Paragraph("Net Salary in Words:", netPayLabel));
            netWordsCell.addElement(new Paragraph(NumberToWordsConverter.convert(record.getNetSalary()), boldFont));
            netTable.addCell(netWordsCell);

            PdfPCell netAmountCell = new PdfPCell();
            netAmountCell.setBackgroundColor(lightGray);
            netAmountCell.setPadding(10);
            netAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph pNetLabel = new Paragraph("TAKE HOME NET SALARY", netPayLabel);
            pNetLabel.setAlignment(Element.ALIGN_RIGHT);
            Paragraph pNetAmount = new Paragraph("INR " + formatCurrency(record.getNetSalary()), netPayFont);
            pNetAmount.setAlignment(Element.ALIGN_RIGHT);
            netAmountCell.addElement(pNetLabel);
            netAmountCell.addElement(pNetAmount);
            netTable.addCell(netAmountCell);

            document.add(netTable);

            // 5. Signatures and Disclaimers
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{50, 50});
            footerTable.setSpacingBefore(30);

            PdfPCell leftSig = new PdfPCell();
            leftSig.setBorder(Rectangle.NO_BORDER);
            leftSig.addElement(new Paragraph("___________________________\nEmployee Signature", regularFont));
            footerTable.addCell(leftSig);

            PdfPCell rightSig = new PdfPCell();
            rightSig.setBorder(Rectangle.NO_BORDER);
            rightSig.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph pAuth = new Paragraph("___________________________\nAuthorized Signatory (HR / Finance)", regularFont);
            pAuth.setAlignment(Element.ALIGN_RIGHT);
            rightSig.addElement(pAuth);
            footerTable.addCell(rightSig);

            document.add(footerTable);

            // Final note
            Paragraph note = new Paragraph("\n\n* This is a computer generated payslip and requires no physical seal if verified electronically.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
            note.setAlignment(Element.ALIGN_CENTER);
            document.add(note);

            document.close();
        } catch (Exception ex) {
            throw new RuntimeException("Error generating payslip PDF: " + ex.getMessage(), ex);
        }

        return out.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valFont, Color bg) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, labelFont));
        cLabel.setBackgroundColor(bg);
        cLabel.setPadding(5);
        table.addCell(cLabel);

        PdfPCell cVal = new PdfPCell(new Phrase(value != null ? value : "", valFont));
        cVal.setPadding(5);
        table.addCell(cVal);
    }

    private void addHeaderCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addSalaryRow(PdfPTable table, String earnLabel, BigDecimal earnVal,
                              String dedLabel, BigDecimal dedVal, Font font, Color border) {
        PdfPCell c1 = new PdfPCell(new Phrase(earnLabel, font));
        c1.setPadding(5);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(earnVal != null ? formatCurrency(earnVal) : "-", font));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(5);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(dedLabel, font));
        c3.setPadding(5);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(dedVal != null ? formatCurrency(dedVal) : "-", font));
        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c4.setPadding(5);
        table.addCell(c4);
    }

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? String.format("%,.2f", amount) : "0.00";
    }

    private String getMonthName(int month) {
        String[] months = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return (month >= 1 && month <= 12) ? months[month] : String.valueOf(month);
    }
}
