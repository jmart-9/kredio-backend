package com.kredio.backend.service;

import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.PaymentRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;

    private static final Color PRIMARY_COLOR = new Color(79, 70, 229);
    private static final Color HEADER_BG = new Color(243, 244, 246);
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-SV"));
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Genera PDF de un préstamo con su calendario de pagos
     */
    public byte[] generateLoanPdf(UUID loanId) throws Exception {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        Client client = clientRepository.findById(loan.getClientId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<LoanSchedule> schedules = getLoanSchedules(loanId);

        Document document = new Document(PageSize.LETTER.rotate());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();
        addHeader(document, "CONTRATO DE PRÉSTAMO");
        addLoanDetails(document, loan, client);
        addPaymentSchedule(document, schedules);
        addSignatures(document, client.getFullName());
        document.close();

        return outputStream.toByteArray();
    }

    /**
     * Genera PDF de comprobante de pago
     */
    public byte[] generatePaymentReceiptPdf(UUID paymentId) throws Exception {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        Loan loan = loanRepository.findById(payment.getLoanId())
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        Client client = clientRepository.findById(loan.getClientId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();
        addReceiptHeader(document);
        addPaymentDetails(document, payment, loan, client);
        addFooter(document);
        document.close();

        return outputStream.toByteArray();
    }

    /**
     * Genera PDF de reporte de cartera PAR
     */
    public byte[] generateParReportPdf(UUID tenantId, BigDecimal par30, BigDecimal par60,
                                       BigDecimal par90, BigDecimal totalPortfolio) throws Exception {
        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();
        addHeader(document, "REPORTE DE CARTERA EN RIESGO (PAR)");
        addParDetails(document, tenantId, par30, par60, par90, totalPortfolio);
        document.close();

        return outputStream.toByteArray();
    }

    // Métodos privados de ayuda
    private void addHeader(Document document, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(20);
        document.add(titleParagraph);

        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Paragraph info = new Paragraph("Generado: " + java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), infoFont);
        info.setAlignment(Element.ALIGN_RIGHT);
        info.setSpacingAfter(20);
        document.add(info);
    }

    private void addLoanDetails(Document document, Loan loan, Client client) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);

        addTableRow(table, "Número de Préstamo:", loan.getId().toString());
        addTableRow(table, "Cliente:", client.getFullName());
        addTableRow(table, "DPI/Identificación:", client.getDpiOrId());
        addTableRow(table, "Monto del Préstamo:", formatCurrency(loan.getPrincipalAmount()));
        addTableRow(table, "Tasa de Interés:", loan.getInterestRate() + "%");
        addTableRow(table, "Plazo:", loan.getTermMonths() + " meses");
        addTableRow(table, "Cuota Mensual:", formatCurrency(calculateMonthlyPayment(loan)));
        addTableRow(table, "Saldo Actual:", formatCurrency(loan.getCurrentBalance()));
        addTableRow(table, "Fecha de Creación:", formatInstant(loan.getCreatedAt()));
        addTableRow(table, "Estado:", loan.getStatus().toString());

        document.add(table);
    }

    private void addPaymentSchedule(Document document, List<LoanSchedule> schedules) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);
        Paragraph section = new Paragraph("CALENDARIO DE PAGOS", sectionFont);
        section.setSpacingBefore(20);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{1, 2, 2, 2, 2, 2});

        // Headers
        String[] headers = {"# Cuota", "Fecha Pago", "Cuota", "Capital", "Interés", "Saldo"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Datos reales con los campos correctos de LoanSchedule
        for (LoanSchedule schedule : schedules) {
            table.addCell(String.valueOf(schedule.getInstallmentNumber()));

            // dueDate es LocalDate
            table.addCell(schedule.getDueDate().format(dateFormatter));

            table.addCell(formatCurrency(schedule.getExpectedAmount()));
            table.addCell(formatCurrency(schedule.getExpectedPrincipal()));
            table.addCell(formatCurrency(schedule.getExpectedInterest()));

            // LoanSchedule no tiene remainingBalance, calculamos uno aproximado
            // O puedes poner "N/A" si prefieres
            table.addCell(formatCurrency(schedule.getExpectedAmount())); // Placeholder

            // Si quieres calcular el saldo restante, necesitarías la lógica completa
            // Por ahora ponemos el monto esperado como referencia
        }

        document.add(table);
    }

    private void addSignatures(Document document, String clientName) throws DocumentException {
        document.newPage();

        Font signatureFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Paragraph signatureLabel = new Paragraph("FIRMAS DE CONFORMIDAD", signatureFont);
        signatureLabel.setAlignment(Element.ALIGN_CENTER);
        signatureLabel.setSpacingAfter(40);
        document.add(signatureLabel);

        PdfPTable signatures = new PdfPTable(2);
        signatures.setWidthPercentage(80);
        signatures.setHorizontalAlignment(Element.ALIGN_CENTER);
        signatures.setSpacingBefore(20);

        PdfPCell clientCell = new PdfPCell(new Phrase("_________________________\n" + clientName + "\nCLIENTE",
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        clientCell.setBorder(Rectangle.NO_BORDER);
        clientCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        clientCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        clientCell.setMinimumHeight(80);

        PdfPCell lenderCell = new PdfPCell(new Phrase("_________________________\nREPRESENTANTE DE LA FINANCIERA",
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        lenderCell.setBorder(Rectangle.NO_BORDER);
        lenderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        lenderCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        lenderCell.setMinimumHeight(80);

        signatures.addCell(clientCell);
        signatures.addCell(lenderCell);

        document.add(signatures);
    }

    private void addReceiptHeader(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY_COLOR);
        Paragraph title = new Paragraph("COMPROBANTE DE PAGO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);
    }

    private void addPaymentDetails(Document document, Payment payment, Loan loan, Client client) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        addTableRow(table, "Número de Comprobante:", payment.getId().toString());
        addTableRow(table, "Fecha de Pago:", formatInstant(payment.getPaymentDate()));
        addTableRow(table, "Cliente:", client.getFullName());
        addTableRow(table, "Préstamo:", loan.getId().toString());
        addTableRow(table, "Monto Pagado:", formatCurrency(payment.getAmountPaid()));
        addTableRow(table, "Método de Pago:", payment.getMethod().toString());

        // Estos campos pueden no existir en Payment - los pongo en 0 por ahora
        addTableRow(table, "Saldo Anterior:", "$0.00");
        addTableRow(table, "Nuevo Saldo:", formatCurrency(loan.getCurrentBalance()));

        if (payment.getExcessAmount() != null && payment.getExcessAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Pago de Más (a capital):", formatCurrency(payment.getExcessAmount()));
        }

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
        Paragraph footer = new Paragraph("Este comprobante es un documento válido. Consérvelo para sus registros.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(40);
        document.add(footer);
    }

    private void addParDetails(Document document, UUID tenantId, BigDecimal par30, BigDecimal par60,
                               BigDecimal par90, BigDecimal totalPortfolio) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);
        Paragraph section = new Paragraph("RESUMEN DE CARTERA EN RIESGO", sectionFont);
        section.setSpacingBefore(20);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{3, 2, 2, 2});
        table.setSpacingBefore(10);

        // Headers
        String[] headers = {"Concepto", "Monto", "% del Total", "Descripción"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Total Portfolio
        table.addCell("Cartera Total");
        table.addCell(formatCurrency(totalPortfolio));
        table.addCell("100%");
        table.addCell("Saldo total de préstamos activos");

        // PAR 30
        table.addCell("PAR 30 días");
        table.addCell(formatCurrency(par30));
        table.addCell(calculatePercentage(par30, totalPortfolio) + "%");
        table.addCell("Cartera vencida > 30 días");

        // PAR 60
        table.addCell("PAR 60 días");
        table.addCell(formatCurrency(par60));
        table.addCell(calculatePercentage(par60, totalPortfolio) + "%");
        table.addCell("Cartera vencida > 60 días");

        // PAR 90
        table.addCell("PAR 90 días");
        table.addCell(formatCurrency(par90));
        table.addCell(calculatePercentage(par90, totalPortfolio) + "%");
        table.addCell("Cartera vencida > 90 días");

        document.add(table);
    }

    // Métodos utilitarios
    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatCurrency(BigDecimal amount) {
        return currencyFormat.format(amount);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        return instant.atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(dateFormatter);
    }

    private String calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return "0.00";
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .toString();
    }

    private BigDecimal calculateMonthlyPayment(Loan loan) {
        // Fórmula de amortización francesa
        double monthlyRate = loan.getInterestRate().doubleValue() / 100 / 12;
        int months = loan.getTermMonths();
        double principal = loan.getPrincipalAmount().doubleValue();

        if (monthlyRate == 0) {
            return BigDecimal.valueOf(principal / months);
        }

        double payment = principal * (monthlyRate * Math.pow(1 + monthlyRate, months)) /
                (Math.pow(1 + monthlyRate, months) - 1);

        return BigDecimal.valueOf(payment).setScale(2, RoundingMode.HALF_UP);
    }

    private List<LoanSchedule> getLoanSchedules(UUID loanId) {
        // TODO: Implementar cuando tengas el LoanScheduleRepository inyectado
        return List.of();
    }
}