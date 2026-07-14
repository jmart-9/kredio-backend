package com.kredio.backend.controller;

import com.kredio.backend.dto.ClientResponse;
import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClientQueryController {

    private final ClientRepository clientRepository;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) String search) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<Client> clients;

        if (search != null && !search.isEmpty()) {
            clients = clientRepository.findByTenantIdAndSearch(tenantId, search);
        } else {
            clients = clientRepository.findByTenantId(tenantId);
        }

        List<ClientResponse> response = clients.stream()
                .map(client -> new ClientResponse(
                        client.getId(),
                        client.getFullName(),
                        client.getDpiOrId(),
                        client.getPhone(),
                        client.getAddress(),
                        client.getPortfolioId()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!client.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado: el cliente no pertenece a esta financiera");
        }

        return ResponseEntity.ok(client);
    }

    @PostMapping
    public ResponseEntity<Client> createClient(
            @RequestBody Client client,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        client.setTenantId(tenantId);

        // ✅ EL portfolioId YA VIENE EN EL OBJETO client, se guarda automáticamente
        Client savedClient = clientRepository.save(client);
        return ResponseEntity.ok(savedClient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(
            @PathVariable UUID id,
            @RequestBody Client client,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!existingClient.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado: el cliente no pertenece a esta financiera");
        }

        existingClient.setFullName(client.getFullName());
        existingClient.setPhone(client.getPhone());
        existingClient.setEmail(client.getEmail());
        existingClient.setAddress(client.getAddress());

        // ✅ AGREGAR ESTA LÍNEA
        existingClient.setPortfolioId(client.getPortfolioId());

        Client updatedClient = clientRepository.save(existingClient);
        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteClient(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!client.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado: el cliente no pertenece a esta financiera");
        }

        // PASO 1: Obtener todos los préstamos del cliente
        List<Loan> loans = loanRepository.findByTenantIdAndClientId(tenantId, id);

        for (Loan loan : loans) {
            // PASO 2: Eliminar pagos asociados al préstamo
            List<Payment> payments = paymentRepository.findByLoanId(loan.getId());
            paymentRepository.deleteAll(payments);

            // PASO 3: Eliminar calendarios de pagos del préstamo
            List<LoanSchedule> schedules = loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
            loanScheduleRepository.deleteAll(schedules);
        }

        // PASO 4: Eliminar todos los préstamos del cliente
        loanRepository.deleteAll(loans);

        // PASO 5: Eliminar el cliente
        clientRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }
}