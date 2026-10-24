package com.kredio.backend.service;

import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.repository.PaymentRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final LoanScheduleRepository scheduleRepository;

    // ==================== PDF ====================

    public byte[] generateClientPDF(UUID clientId, UUID tenantId) throws Exception {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!client.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        List<Loan> loans = loanRepository.findByTenantIdAndClientId(tenantId, clientId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Título
        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("DETALLE DEL CLIENTE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // Información del cliente
        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(100);
        clientTable.setSpacingBefore(10f);
        clientTable.setSpacingAfter(10f);

        addCell(clientTable, "Nombre:", FontFactory.HELVETICA_BOLD);
        addCell(clientTable, client.getFullName(), FontFactory.HELVETICA);
        addCell(clientTable, "DUI/ID:", FontFactory.HELVETICA_BOLD);
        addCell(clientTable, client.getDpiOrId(), FontFactory.HELVETICA);
        addCell(clientTable, "Teléfono:", FontFactory.HELVETICA_BOLD);
        addCell(clientTable, client.getPhone() != null ? client.getPhone() : "N/A", FontFactory.HELVETICA);
        addCell(clientTable, "Email:", FontFactory.HELVETICA_BOLD);
        addCell(clientTable, client.getEmail() != null ? client.getEmail() : "N/A", FontFactory.HELVETICA);
        addCell(clientTable, "Dirección:", FontFactory.HELVETICA_BOLD);
        addCell(clientTable, client.getAddress() != null ? client.getAddress() : "N/A", FontFactory.HELVETICA);

        document.add(clientTable);

        // Préstamos
        document.add(new Paragraph("\n"));
        com.lowagie.text.Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        document.add(new Paragraph("PRÉSTAMOS", subtitleFont));
        document.add(new Paragraph("\n"));

        PdfPTable loansTable = new PdfPTable(new float[]{1f, 2f, 2f, 2f, 2f, 2f});
        loansTable.setWidthPercentage(100);
        loansTable.setSpacingBefore(10f);

        // Headers
        String[] headers = {"Monto", "Tasa", "Plazo", "Saldo", "Estado", "Fecha"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            loansTable.addCell(cell);
        }

        // Datos
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Loan loan : loans) {
            loansTable.addCell(formatCurrency(loan.getPrincipalAmount()));
            loansTable.addCell(loan.getInterestRate() + "%");
            loansTable.addCell(String.valueOf(loan.getTermMonths()));
            loansTable.addCell(formatCurrency(loan.getCurrentBalance()));
            loansTable.addCell(loan.getStatus().name());
            loansTable.addCell(loan.getOpeningDate() != null ?
                    loan.getOpeningDate().format(formatter) : "N/A");
        }

        document.add(loansTable);

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateLoanSchedulePDF(UUID loanId, UUID tenantId) throws Exception {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        Client client = clientRepository.findById(loan.getClientId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<LoanSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);

        document.open();

        // Título
        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph title = new Paragraph("CALENDARIO DE PAGOS", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // Info del préstamo
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        addCell(infoTable, "Cliente:", FontFactory.HELVETICA_BOLD);
        addCell(infoTable, client.getFullName(), FontFactory.HELVETICA);
        addCell(infoTable, "Monto:", FontFactory.HELVETICA_BOLD);
        addCell(infoTable, formatCurrency(loan.getPrincipalAmount()), FontFactory.HELVETICA);
        addCell(infoTable, "Tasa:", FontFactory.HELVETICA_BOLD);
        addCell(infoTable, loan.getInterestRate() + "%", FontFactory.HELVETICA);
        addCell(infoTable, "Plazo:", FontFactory.HELVETICA_BOLD);
        addCell(infoTable, String.valueOf(loan.getTermMonths()) + " meses", FontFactory.HELVETICA);

        document.add(infoTable);
        document.add(new Paragraph("\n"));

        // Tabla de calendario
        PdfPTable scheduleTable = new PdfPTable(new float[]{1f, 2f, 2f, 2f, 2f, 2f, 2f});
        scheduleTable.setWidthPercentage(100);
        scheduleTable.setSpacingBefore(10f);

        String[] headers = {"No.", "Vencimiento", "Cuota", "Capital", "Interés", "Estado", "Fecha Pago"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            scheduleTable.addCell(cell);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (LoanSchedule schedule : schedules) {
            scheduleTable.addCell(String.valueOf(schedule.getInstallmentNumber()));
            scheduleTable.addCell(schedule.getDueDate().format(formatter));
            scheduleTable.addCell(formatCurrency(schedule.getExpectedAmount()));
            scheduleTable.addCell(formatCurrency(schedule.getExpectedPrincipal()));
            scheduleTable.addCell(formatCurrency(schedule.getExpectedInterest()));
            scheduleTable.addCell(schedule.getStatus().name());
            scheduleTable.addCell("N/A"); // No hay paidDate en LoanSchedule
        }

        document.add(scheduleTable);
        document.close();
        return baos.toByteArray();
    }

    // ==================== EXCEL ====================

    public byte[] generateClientsExcel(UUID tenantId) throws Exception {
        List<Client> clients = clientRepository.findByTenantId(tenantId);

        Workbook workbook = new XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Clientes");

        // Header
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Nombre", "DUI/ID", "Teléfono", "Email", "Dirección", "Fecha Creación"};

        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        // Datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Client client : clients) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(client.getId().toString());
            row.createCell(1).setCellValue(client.getFullName());
            row.createCell(2).setCellValue(client.getDpiOrId());
            row.createCell(3).setCellValue(client.getPhone() != null ? client.getPhone() : "");
            row.createCell(4).setCellValue(client.getEmail() != null ? client.getEmail() : "");
            row.createCell(5).setCellValue(client.getAddress() != null ? client.getAddress() : "");
            row.createCell(6).setCellValue(client.getCreatedAt() != null ?
                    Instant.ofEpochMilli(client.getCreatedAt().toEpochMilli())
                            .atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter) : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    public byte[] generateLoansExcel(UUID tenantId) throws Exception {
        List<Loan> loans = loanRepository.findByTenantId(tenantId);

        Workbook workbook = new XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Préstamos");

        // Header
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Cliente", "Monto", "Tasa", "Plazo", "Saldo", "Estado", "Fecha Apertura"};

        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        // Datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Loan loan : loans) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            Client client = clientRepository.findById(loan.getClientId()).orElse(null);

            row.createCell(0).setCellValue(loan.getId().toString());
            row.createCell(1).setCellValue(client != null ? client.getFullName() : "N/A");
            row.createCell(2).setCellValue(loan.getPrincipalAmount().doubleValue());
            row.createCell(3).setCellValue(loan.getInterestRate().doubleValue());
            row.createCell(4).setCellValue(loan.getTermMonths());
            row.createCell(5).setCellValue(loan.getCurrentBalance().doubleValue());
            row.createCell(6).setCellValue(loan.getStatus().name());
            row.createCell(7).setCellValue(loan.getOpeningDate() != null ?
                    loan.getOpeningDate().format(formatter) : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    public byte[] generatePaymentsReportExcel(UUID tenantId, Instant startDate, Instant endDate) throws Exception {
        List<Payment> payments = paymentRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate);

        Workbook workbook = new XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Pagos");

        // Header
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"Fecha", "Préstamo", "Cliente", "Monto", "Método", "Cobrador"};

        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        // Datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Payment payment : payments) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            Loan loan = loanRepository.findById(payment.getLoanId()).orElse(null);
            Client client = loan != null ? clientRepository.findById(loan.getClientId()).orElse(null) : null;

            row.createCell(0).setCellValue(payment.getPaymentDate() != null ?
                    Instant.ofEpochMilli(payment.getPaymentDate().toEpochMilli())
                            .atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter) : "");
            row.createCell(1).setCellValue(loan != null ? loan.getId().toString() : "N/A");
            row.createCell(2).setCellValue(client != null ? client.getFullName() : "N/A");
            row.createCell(3).setCellValue(payment.getAmountPaid().doubleValue());
            row.createCell(4).setCellValue(payment.getMethod().name());
            row.createCell(5).setCellValue(payment.getCollectorId().toString());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    // ==================== UTILS ====================

    private void addCell(PdfPTable table, String text, String fontName) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(fontName, 10)));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal amount) {
        return "$" + amount.toString();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}