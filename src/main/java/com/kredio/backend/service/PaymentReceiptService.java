package com.kredio.backend.service;

import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.entity.Tenant;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.repository.PaymentRepository;
import com.kredio.backend.repository.TenantRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReceiptService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository scheduleRepository;
    private final TenantRepository tenantRepository;

    public byte[] generateReceiptPdf(UUID paymentId, UUID tenantId) {
        log.info("📄 Generando recibo PDF para pago: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));

        if (!payment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El pago no pertenece a este tenant");
        }

        Loan loan = loanRepository.findById(payment.getLoanId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        LoanSchedule schedule = null;
        if (payment.getScheduleId() != null) {
            schedule = scheduleRepository.findById(payment.getScheduleId()).orElse(null);
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Estilos
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 64, 175));
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(55, 65, 81));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(31, 41, 55));
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(31, 41, 55));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(107, 114, 128));
            Font moneyFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(30, 64, 175));

            // ✅ Obtener símbolo de moneda de forma segura
            String currencySymbol = "$";
            try {
                if (tenant.getCurrencySymbol() != null && !tenant.getCurrencySymbol().isEmpty()) {
                    currencySymbol = tenant.getCurrencySymbol();
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener el símbolo de moneda, usando $ por defecto");
            }

            // ENCABEZADO
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.addElement(new Paragraph(tenant.getName() != null ? tenant.getName() : "Kredio", titleFont));
            if (tenant.getSubdomain() != null) {
                logoCell.addElement(new Paragraph(tenant.getSubdomain(), smallFont));
            }
            headerTable.addCell(logoCell);

            PdfPCell receiptCell = new PdfPCell();
            receiptCell.setBorder(Rectangle.NO_BORDER);
            receiptCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            receiptCell.addElement(new Paragraph("RECIBO DE PAGO", subtitleFont));
            String receiptId = payment.getId().toString().substring(0, 8).toUpperCase();
            receiptCell.addElement(new Paragraph("N° " + receiptId, normalFont));
            headerTable.addCell(receiptCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));
            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorderColorTop(new Color(30, 64, 175));
            lineCell.setBorderWidthTop(2);
            lineCell.setBorder(Rectangle.TOP);
            lineTable.addCell(lineCell);
            document.add(lineTable);
            document.add(new Paragraph(" "));

            // INFORMACIÓN
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});
            infoTable.setSpacingAfter(15);

            PdfPCell clientCell = new PdfPCell();
            clientCell.setBorder(Rectangle.NO_BORDER);
            clientCell.setPadding(5);
            clientCell.addElement(new Paragraph("INFORMACIÓN DEL CLIENTE", subtitleFont));
            clientCell.addElement(new Paragraph(" "));
            clientCell.addElement(new Paragraph("ID Cliente: " + loan.getClientId().toString().substring(0, 8), normalFont));
            clientCell.addElement(new Paragraph("Préstamo ID: " + loan.getId().toString().substring(0, 8), normalFont));
            infoTable.addCell(clientCell);

            PdfPCell paymentCell = new PdfPCell();
            paymentCell.setBorder(Rectangle.NO_BORDER);
            paymentCell.setPadding(5);
            paymentCell.addElement(new Paragraph("DETALLES DEL PAGO", subtitleFont));
            paymentCell.addElement(new Paragraph(" "));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String paymentDate = "N/A";
            if (payment.getPaymentDate() != null) {
                paymentDate = payment.getPaymentDate().atZone(ZoneId.systemDefault()).format(formatter);
            }
            paymentCell.addElement(new Paragraph("Fecha: " + paymentDate, normalFont));
            paymentCell.addElement(new Paragraph("Método: " + (payment.getMethod() != null ? payment.getMethod().name() : "N/A"), normalFont));
            if (schedule != null) {
                paymentCell.addElement(new Paragraph("Cuota N°: " + schedule.getInstallmentNumber(), normalFont));
            }
            infoTable.addCell(paymentCell);
            document.add(infoTable);

            // DESGLOSE
            document.add(new Paragraph("DESGLOSE DEL PAGO", subtitleFont));
            document.add(new Paragraph(" "));

            PdfPTable breakdownTable = new PdfPTable(2);
            breakdownTable.setWidthPercentage(100);
            breakdownTable.setWidths(new float[]{60, 40});
            breakdownTable.setSpacingAfter(15);

            PdfPCell headerConcept = new PdfPCell(new Paragraph("CONCEPTO", boldFont));
            headerConcept.setBackgroundColor(new Color(243, 244, 246));
            headerConcept.setPadding(8);
            breakdownTable.addCell(headerConcept);

            PdfPCell headerAmount = new PdfPCell(new Paragraph("MONTO", boldFont));
            headerAmount.setBackgroundColor(new Color(243, 244, 246));
            headerAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerAmount.setPadding(8);
            breakdownTable.addCell(headerAmount);

            // ✅ Safe getters con null checks
            BigDecimal amountPaid = payment.getAmountPaid() != null ? payment.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal moratory = payment.getAppliedToMoratory() != null ? payment.getAppliedToMoratory() : BigDecimal.ZERO;
            BigDecimal interest = payment.getAppliedToInterest() != null ? payment.getAppliedToInterest() : BigDecimal.ZERO;
            BigDecimal principal = payment.getAppliedToPrincipal() != null ? payment.getAppliedToPrincipal() : BigDecimal.ZERO;
            BigDecimal excess = payment.getExcessAmount() != null ? payment.getExcessAmount() : BigDecimal.ZERO;

            addBreakdownRow(breakdownTable, "Monto Pagado", amountPaid, moneyFont, normalFont, currencySymbol);
            addBreakdownRow(breakdownTable, "A Mora", moratory, normalFont, normalFont, currencySymbol);
            addBreakdownRow(breakdownTable, "A Interés", interest, normalFont, normalFont, currencySymbol);
            addBreakdownRow(breakdownTable, "A Capital", principal, normalFont, normalFont, currencySymbol);

            if (excess.compareTo(BigDecimal.ZERO) > 0) {
                addBreakdownRow(breakdownTable, "Excedente", excess, normalFont, normalFont, currencySymbol);
            }

            document.add(breakdownTable);

            // SALDOS
            document.add(new Paragraph("SALDOS", subtitleFont));
            document.add(new Paragraph(" "));

            PdfPTable balanceTable = new PdfPTable(2);
            balanceTable.setWidthPercentage(100);
            balanceTable.setWidths(new float[]{60, 40});
            balanceTable.setSpacingAfter(20);

            PdfPCell balHeader1 = new PdfPCell(new Paragraph("CONCEPTO", boldFont));
            balHeader1.setBackgroundColor(new Color(243, 244, 246));
            balHeader1.setPadding(8);
            balanceTable.addCell(balHeader1);

            PdfPCell balHeader2 = new PdfPCell(new Paragraph("MONTO", boldFont));
            balHeader2.setBackgroundColor(new Color(243, 244, 246));
            balHeader2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            balHeader2.setPadding(8);
            balanceTable.addCell(balHeader2);

            // ✅ Cálculo seguro de saldos
            BigDecimal currentBalance = loan.getCurrentBalance() != null ? loan.getCurrentBalance() : BigDecimal.ZERO;
            BigDecimal previousBalance = currentBalance.add(principal);

            addBalanceRow(balanceTable, "Saldo Anterior", previousBalance, normalFont, currencySymbol);
            addBalanceRow(balanceTable, "Saldo Actual", currentBalance, boldFont, currencySymbol);

            document.add(balanceTable);

            // PIE DE PÁGINA
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            Paragraph footerText = new Paragraph(
                    "Este documento es un comprobante oficial de pago generado electrónicamente.",
                    smallFont
            );
            footerText.setAlignment(Element.ALIGN_CENTER);
            document.add(footerText);

            Paragraph footerText2 = new Paragraph(
                    "Generado el " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                            " por " + (tenant.getName() != null ? tenant.getName() : "Kredio"),
                    smallFont
            );
            footerText2.setAlignment(Element.ALIGN_CENTER);
            document.add(footerText2);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            log.info("✅ Recibo PDF generado exitosamente: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error(" Error al generar recibo PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el recibo: " + e.getMessage());
        }
    }

    private void addBreakdownRow(PdfPTable table, String concept, BigDecimal amount,
                                 Font amountFont, Font conceptFont, String currencySymbol) {
        PdfPCell conceptCell = new PdfPCell(new Paragraph(concept, conceptFont));
        conceptCell.setBorder(Rectangle.BOTTOM);
        conceptCell.setBorderColorBottom(new Color(229, 231, 235));
        conceptCell.setPadding(6);
        table.addCell(conceptCell);

        String amountStr = currencySymbol + " " + formatMoney(amount);
        PdfPCell amountCell = new PdfPCell(new Paragraph(amountStr, amountFont));
        amountCell.setBorder(Rectangle.BOTTOM);
        amountCell.setBorderColorBottom(new Color(229, 231, 235));
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setPadding(6);
        table.addCell(amountCell);
    }

    private void addBalanceRow(PdfPTable table, String concept, BigDecimal amount,
                               Font font, String currencySymbol) {
        PdfPCell conceptCell = new PdfPCell(new Paragraph(concept, font));
        conceptCell.setBorder(Rectangle.BOTTOM);
        conceptCell.setBorderColorBottom(new Color(229, 231, 235));
        conceptCell.setPadding(6);
        table.addCell(conceptCell);

        String amountStr = currencySymbol + " " + formatMoney(amount);
        PdfPCell amountCell = new PdfPCell(new Paragraph(amountStr, font));
        amountCell.setBorder(Rectangle.BOTTOM);
        amountCell.setBorderColorBottom(new Color(229, 231, 235));
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setPadding(6);
        table.addCell(amountCell);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}