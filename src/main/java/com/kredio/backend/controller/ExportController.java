package com.kredio.backend.controller;

import com.kredio.backend.entity.Client;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ExportController {

    private final ReportService reportService;
    private final ClientRepository clientRepository;

    // ==================== PDF ====================

    @GetMapping("/clients/{clientId}/pdf")
    public ResponseEntity<byte[]> generateClientPDF(
            @PathVariable UUID clientId,
            HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] pdf = reportService.generateClientPDF(clientId, tenantId);

            Client client = clientRepository.findById(clientId).orElse(null);
            String fileName = "cliente_" + (client != null ? client.getFullName().replaceAll("\\s+", "_") : clientId.toString().substring(0, 8)) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/loans/{loanId}/pdf")
    public ResponseEntity<byte[]> generateLoanSchedulePDF(
            @PathVariable UUID loanId,
            HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] pdf = reportService.generateLoanSchedulePDF(loanId, tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "calendario_" + loanId.toString().substring(0, 8) + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== EXCEL ====================

    @GetMapping("/clients/excel")
    public ResponseEntity<byte[]> generateClientsExcel(HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] excel = reportService.generateClientsExcel(tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "clientes_" + LocalDate.now() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excel);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/loans/excel")
    public ResponseEntity<byte[]> generateLoansExcel(HttpServletRequest httpRequest) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] excel = reportService.generateLoansExcel(tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "prestamos_" + LocalDate.now() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excel);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/payments/excel")
    public ResponseEntity<byte[]> generatePaymentsExcel(
            HttpServletRequest httpRequest,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        try {
            UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
            byte[] excel = reportService.generatePaymentsReportExcel(tenantId, startDate, endDate);

            String dateFrom = Instant.ofEpochMilli(startDate.toEpochMilli())
                    .atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String dateTo = Instant.ofEpochMilli(endDate.toEpochMilli())
                    .atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment",
                    "pagos_" + dateFrom + "_al_" + dateTo + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excel);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}