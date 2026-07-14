package com.kredio.backend.service;

import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.PaymentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Genera Excel de préstamos con datos reales
     */
    public byte[] generateLoansExcel(UUID tenantId) throws Exception {
        List<Loan> loans = loanRepository.findByTenantId(tenantId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Préstamos");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        // Crear header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Cliente", "Monto Original", "Saldo Actual", "Tasa Interés",
                "Plazo (meses)", "Fecha Inicio", "Estado"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Agregar datos reales
        int rowNum = 1;
        for (Loan loan : loans) {
            Client client = clientRepository.findById(loan.getClientId()).orElse(null);
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(loan.getId().toString());
            row.createCell(1).setCellValue(client != null ? client.getFullName() : "N/A");
            row.createCell(2).setCellValue(loan.getPrincipalAmount().doubleValue());
            row.createCell(3).setCellValue(loan.getCurrentBalance().doubleValue());
            row.createCell(4).setCellValue(loan.getInterestRate().doubleValue());
            row.createCell(5).setCellValue(loan.getTermMonths());
            row.createCell(6).setCellValue(formatInstant(loan.getCreatedAt()));
            row.createCell(7).setCellValue(loan.getStatus().toString());
        }

        // Ajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Genera Excel de pagos con datos reales
     */
    public byte[] generatePaymentsExcel(UUID tenantId) throws Exception {
        List<Payment> payments = paymentRepository.findByTenantId(tenantId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Pagos");

        CellStyle headerStyle = createHeaderStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID Pago", "Préstamo", "Cliente", "Fecha", "Monto",
                "Método", "Saldo Anterior", "Nuevo Saldo"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos reales
        int rowNum = 1;
        for (Payment payment : payments) {
            Loan loan = loanRepository.findById(payment.getLoanId()).orElse(null);
            Client client = null;
            if (loan != null) {
                client = clientRepository.findById(loan.getClientId()).orElse(null);
            }

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(payment.getId().toString());
            row.createCell(1).setCellValue(payment.getLoanId().toString());
            row.createCell(2).setCellValue(client != null ? client.getFullName() : "N/A");
            row.createCell(3).setCellValue(formatInstant(payment.getPaymentDate()));
            row.createCell(4).setCellValue(payment.getAmountPaid().doubleValue());
            row.createCell(5).setCellValue(payment.getMethod().toString());

            // Si existen los campos, los usamos; si no, ponemos 0
            // NOTA: Ajusta esto según los campos reales que tengas en Payment
            row.createCell(6).setCellValue(0.0); // previousBalance - eliminar si no existe
            row.createCell(7).setCellValue(0.0); // newBalance - eliminar si no existe
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Genera Excel de clientes con datos reales
     */
    public byte[] generateClientsExcel(UUID tenantId) throws Exception {
        List<Client> clients = clientRepository.findByTenantId(tenantId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Clientes");

        CellStyle headerStyle = createHeaderStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Nombre Completo", "DPI/Identificación",
                "Teléfono", "Dirección", "Fecha Registro"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos reales
        int rowNum = 1;
        for (Client client : clients) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(client.getId().toString());
            row.createCell(1).setCellValue(client.getFullName());
            row.createCell(2).setCellValue(client.getDpiOrId());
            row.createCell(3).setCellValue(client.getPhone() != null ? client.getPhone() : "");
            row.createCell(4).setCellValue(client.getAddress() != null ? client.getAddress() : "");
            row.createCell(5).setCellValue(formatInstant(client.getCreatedAt()));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Genera Excel de reporte PAR
     */
    public byte[] generateParReportExcel(UUID tenantId, BigDecimal par30, BigDecimal par60,
                                         BigDecimal par90, BigDecimal totalPortfolio) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte PAR");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);

        // Título
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE CARTERA EN RIESGO (PAR)");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        // Fecha
        Row dateRow = sheet.createRow(1);
        dateRow.createCell(0).setCellValue("Fecha: " + java.time.LocalDate.now().format(dateFormatter));
        dateRow.createCell(2).setCellValue("Tenant ID: " + tenantId.toString());

        // Headers
        Row headerData = sheet.createRow(3);
        String[] colHeaders = {"Concepto", "Monto", "% del Total", "Descripción"};

        for (int i = 0; i < colHeaders.length; i++) {
            Cell cell = headerData.createCell(i);
            cell.setCellValue(colHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 4;

        // Total Portfolio
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("Cartera Total");
        totalRow.createCell(1).setCellValue(totalPortfolio.doubleValue());
        totalRow.createCell(2).setCellValue("100.00%");
        totalRow.createCell(3).setCellValue("Saldo total de préstamos activos");

        // PAR 30
        Row par30Row = sheet.createRow(rowNum++);
        par30Row.createCell(0).setCellValue("PAR 30 días");
        par30Row.createCell(1).setCellValue(par30.doubleValue());
        par30Row.createCell(2).setCellValue(calculatePercentage(par30, totalPortfolio) + "%");
        par30Row.createCell(3).setCellValue("Cartera vencida > 30 días");

        // PAR 60
        Row par60Row = sheet.createRow(rowNum++);
        par60Row.createCell(0).setCellValue("PAR 60 días");
        par60Row.createCell(1).setCellValue(par60.doubleValue());
        par60Row.createCell(2).setCellValue(calculatePercentage(par60, totalPortfolio) + "%");
        par60Row.createCell(3).setCellValue("Cartera vencida > 60 días");

        // PAR 90
        Row par90Row = sheet.createRow(rowNum++);
        par90Row.createCell(0).setCellValue("PAR 90 días");
        par90Row.createCell(1).setCellValue(par90.doubleValue());
        par90Row.createCell(2).setCellValue(calculatePercentage(par90, totalPortfolio) + "%");
        par90Row.createCell(3).setCellValue("Cartera vencida > 90 días");

        // Ajustar columnas
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // Métodos de ayuda para estilos
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private String calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return "0.00";
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, java.math.RoundingMode.HALF_UP)
                .toString();
    }

    /**
     * Helper method to format Instant to String
     */
    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        return instant.atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(dateFormatter);
    }
}