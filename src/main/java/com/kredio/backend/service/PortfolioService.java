package com.kredio.backend.service;

import com.kredio.backend.dto.ClientResponse;
import com.kredio.backend.dto.PortfolioMetrics;
import com.kredio.backend.dto.PortfolioResponse;
import com.kredio.backend.entity.Client;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.Portfolio;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import com.kredio.backend.repository.PortfolioRepository;
import com.kredio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    // ==========================================
    // MÉTRICAS GENERALES (Dashboard consolidado)
    // ==========================================

    public PortfolioMetrics calculateMetrics(UUID tenantId) {
        List<Loan> loans = loanRepository.findByTenantId(tenantId);
        return calculateMetricsFromLoans(loans);
    }

    // ==========================================
    // MÉTRICAS POR CARTERA ESPECÍFICA
    // ==========================================

    public PortfolioMetrics calculateMetricsByPortfolio(UUID portfolioId, UUID tenantId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Cartera no encontrada"));

        if (!portfolio.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        List<Client> clients = clientRepository.findByPortfolioId(portfolioId);
        List<UUID> clientIds = clients.stream().map(Client::getId).collect(Collectors.toList());

        List<Loan> loans = loanRepository.findByTenantId(tenantId).stream()
                .filter(loan -> clientIds.contains(loan.getClientId()))
                .collect(Collectors.toList());

        return calculateMetricsFromLoans(loans);
    }

    private PortfolioMetrics calculateMetricsFromLoans(List<Loan> loans) {
        BigDecimal totalPortfolio = BigDecimal.ZERO;
        BigDecimal activePortfolio = BigDecimal.ZERO;
        BigDecimal overduePortfolio = BigDecimal.ZERO;
        BigDecimal par30 = BigDecimal.ZERO;
        BigDecimal par60 = BigDecimal.ZERO;
        BigDecimal par90 = BigDecimal.ZERO;

        int totalLoans = loans.size();
        int activeLoans = 0;
        int overdueLoans = 0;
        int completedLoans = 0;

        for (Loan loan : loans) {
            BigDecimal balance = loan.getCurrentBalance() != null ? loan.getCurrentBalance() : BigDecimal.ZERO;
            totalPortfolio = totalPortfolio.add(balance);

            String status = loan.getStatus().toString();

            if ("ACTIVE".equals(status)) {
                activeLoans++;
                activePortfolio = activePortfolio.add(balance);
                par30 = par30.add(balance.multiply(BigDecimal.valueOf(0.1)));
                par60 = par60.add(balance.multiply(BigDecimal.valueOf(0.05)));
                par90 = par90.add(balance.multiply(BigDecimal.valueOf(0.02)));
            } else if ("DEFAULTED".equals(status)) {
                overdueLoans++;
                overduePortfolio = overduePortfolio.add(balance);
                par30 = par30.add(balance);
                par60 = par60.add(balance);
                par90 = par90.add(balance);
            } else if ("COMPLETED".equals(status)) {
                completedLoans++;
            }
        }

        BigDecimal defaultRate = totalLoans > 0
                ? BigDecimal.valueOf(overdueLoans)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalLoans), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PortfolioMetrics.builder()
                .totalPortfolio(totalPortfolio)
                .activePortfolio(activePortfolio)
                .overduePortfolio(overduePortfolio)
                .par30(par30)
                .par60(par60)
                .par90(par90)
                .totalLoans(totalLoans)
                .activeLoans(activeLoans)
                .overdueLoans(overdueLoans)
                .completedLoans(completedLoans)
                .defaultRate(defaultRate)
                .build();
    }

    // ==========================================
    // CRUD DE CARTERAS
    // ==========================================

    public List<PortfolioResponse> getAllPortfolios(UUID tenantId) {
        return portfolioRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PortfolioResponse getPortfolioById(UUID portfolioId, UUID tenantId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Cartera no encontrada"));

        if (!portfolio.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        return mapToResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse createPortfolio(UUID tenantId, String name, String description, UUID assignedUserId) {
        // ✅ Usar el nuevo método que solo busca carteras ACTIVAS
        if (portfolioRepository.findActiveByTenantIdAndName(tenantId, name).isPresent()) {
            throw new RuntimeException("Ya existe una cartera activa con ese nombre");
        }

        Portfolio portfolio = Portfolio.builder()
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .assignedUserId(assignedUserId)
                .isActive(true)
                .build();

        return mapToResponse(portfolioRepository.save(portfolio));
    }

    @Transactional
    public PortfolioResponse updatePortfolio(UUID portfolioId, UUID tenantId, String name, String description, UUID assignedUserId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Cartera no encontrada"));

        if (!portfolio.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        // ✅ Usar el nuevo método que solo busca carteras ACTIVAS
        portfolioRepository.findActiveByTenantIdAndName(tenantId, name)
                .filter(p -> !p.getId().equals(portfolioId))
                .ifPresent(p -> { throw new RuntimeException("Ya existe una cartera activa con ese nombre"); });

        portfolio.setName(name);
        portfolio.setDescription(description);
        portfolio.setAssignedUserId(assignedUserId);

        return mapToResponse(portfolioRepository.save(portfolio));
    }

    @Transactional
    public void deletePortfolio(UUID portfolioId, UUID tenantId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Cartera no encontrada"));

        if (!portfolio.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        int clientCount = portfolioRepository.countClientsByPortfolio(portfolioId);
        if (clientCount > 0) {
            throw new RuntimeException("No se puede eliminar la cartera porque tiene " + clientCount + " clientes asignados");
        }

        portfolio.setIsActive(false);
        portfolioRepository.save(portfolio);
    }

    private PortfolioResponse mapToResponse(Portfolio portfolio) {
        int clientCount = portfolioRepository.countClientsByPortfolio(portfolio.getId());
        int activeLoanCount = portfolioRepository.countActiveLoansByPortfolio(portfolio.getId());

        String assignedUserName = null;
        if (portfolio.getAssignedUserId() != null) {
            assignedUserName = userRepository.findById(portfolio.getAssignedUserId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .assignedUserId(portfolio.getAssignedUserId())
                .assignedUserName(assignedUserName)
                .isActive(portfolio.getIsActive())
                .totalClients(clientCount)
                .activeLoans(activeLoanCount)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(portfolio.getCreatedAt())
                .build();
    }
    public List<ClientResponse> getClientsByPortfolio(UUID portfolioId, UUID tenantId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Cartera no encontrada"));

        if (!portfolio.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        List<Client> clients = clientRepository.findByPortfolioId(portfolioId);
        return clients.stream()
                .map(c -> new ClientResponse(c.getId(), c.getFullName(), c.getDpiOrId(), c.getPhone(), c.getAddress(), c.getPortfolioId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignClientToPortfolio(UUID clientId, UUID portfolioId, UUID tenantId, UUID assignedBy) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Cartera no encontrada"));

        if (!portfolio.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!client.getTenantId().equals(tenantId)) {
            throw new RuntimeException("El cliente no pertenece a este tenant");
        }

        client.setPortfolioId(portfolioId);
        clientRepository.save(client);

        List<Loan> loans = loanRepository.findByTenantIdAndClientId(tenantId, clientId);
        for (Loan loan : loans) {
            loan.setPortfolioId(portfolioId);
            loanRepository.save(loan);
        }
    }
}