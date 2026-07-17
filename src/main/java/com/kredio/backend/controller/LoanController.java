package com.kredio.backend.controller;

import com.kredio.backend.dto.LoanRequest;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.service.LoanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // ✅ ÚNICO ENDPOINT DE ESTE CONTROLADOR: Crear préstamo
    @PostMapping
    @PreAuthorize("hasAuthority('LOAN:CREATE')")
    public ResponseEntity<Loan> createLoan(
            @Valid @RequestBody LoanRequest request,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        Loan loan = loanService.createLoan(request, tenantId);

        return new ResponseEntity<>(loan, HttpStatus.CREATED);
    }
}