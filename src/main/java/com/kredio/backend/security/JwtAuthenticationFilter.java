package com.kredio.backend.security;

import com.kredio.backend.entity.Tenant;
import com.kredio.backend.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TenantRepository tenantRepository; // ✅ AGREGADO

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Ignorar peticiones OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Ignorar endpoints públicos
        if (path.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (jwtUtil.validateToken(token)) {
                UUID userId = jwtUtil.getUserIdFromToken(token);
                UUID tenantId = jwtUtil.getTenantIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                List<String> permissions = jwtUtil.getPermissionsFromToken(token);

                // ✅ NUEVO: Obtener el schemaName del tenant
                String schemaName = tenantRepository.findById(tenantId)
                        .map(Tenant::getSchemaName)
                        .orElse("public");

                List<SimpleGrantedAuthority> authorities = permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                request.setAttribute("tenantId", tenantId);
                request.setAttribute("userId", userId);
                request.setAttribute("role", role);

                // ✅ NUEVO: Establecer el contexto del schema para Hibernate
                TenantContext.setCurrentSchema(schemaName);

                log.debug("Schema establecido para usuario {}: {}", userId, schemaName);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("Token inválido para la ruta: {}", path);
            }
        } catch (Exception e) {
            log.error("Error procesando token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // ✅ Limpiar el contexto al finalizar la petición
            TenantContext.clear();
        }
    }
}