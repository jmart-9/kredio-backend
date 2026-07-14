package com.kredio.backend.controller;

import com.kredio.backend.service.ExcelService;
import com.kredio.backend.service.PdfService;
import com.kredio.backend.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final PdfService pdfService;
    private final ExcelService excelService;
    private final ReportService reportService;

    /**
     * Exportar préstamo a PDF
     */
    @GetMapping("/loans/{id}/pdf")
    public ResponseEntity<byte[]> exportLoanToPdf(@PathVariable UUID id) {
        try {
            byte[] pdfContent = pdfService.generateLoanPdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "prestamo-" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exportar comprobante de pago a PDF
     */
    @GetMapping("/payments/{id}/pdf")
    public ResponseEntity<byte[]> exportPaymentToPdf(@PathVariable UUID id) {
        try {
            byte[] pdfContent = pdfService.generatePaymentReceiptPdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "pago-" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exportar lista de préstamos a Excel
     */
    @GetMapping("/loans/excel")
    public ResponseEntity<byte[]> exportLoansToExcel(HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] excelContent = excelService.generateLoansExcel(tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "prestamos-" + java.time.LocalDate.now() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exportar lista de pagos a Excel
     */
    @GetMapping("/payments/excel")
    public ResponseEntity<byte[]> exportPaymentsToExcel(HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] excelContent = excelService.generatePaymentsExcel(tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "pagos-" + java.time.LocalDate.now() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exportar lista de clientes a Excel
     */
    @GetMapping("/clients/excel")
    public ResponseEntity<byte[]> exportClientsToExcel(HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] excelContent = excelService.generateClientsExcel(tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "clientes-" + java.time.LocalDate.now() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exportar reporte PAR a PDF
     */
    @GetMapping("/reports/par/pdf")
    public ResponseEntity<byte[]> exportParReportToPdf(HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

            // Obtener datos del reporte PAR
            var parReport = reportService.getPortfolioAtRisk(tenantId);

            byte[] pdfContent = pdfService.generateParReportPdf(
                    tenantId,
                    parReport.par30Amount(),
                    parReport.par60Amount(),
                    parReport.par90Amount(),
                    parReport.totalPortfolio()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte-par-" + java.time.LocalDate.now() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exportar reporte PAR a Excel
     */
    @GetMapping("/reports/par/excel")
    public ResponseEntity<byte[]> exportParReportToExcel(HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

            // Obtener datos del reporte PAR
            var parReport = reportService.getPortfolioAtRisk(tenantId);

            byte[] excelContent = excelService.generateParReportExcel(
                    tenantId,
                    parReport.par30Amount(),
                    parReport.par60Amount(),
                    parReport.par90Amount(),
                    parReport.totalPortfolio()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "reporte-par-" + java.time.LocalDate.now() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }
}