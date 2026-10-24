package com.kredio.backend.service;

import com.kredio.backend.entity.User;
import com.kredio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinValidationService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final long LOCKOUT_MINUTES = 15;
    private static final int PIN_LENGTH = 4;

    @Transactional
    public void setPin(UUID userId, String pin) {
        if (pin == null || pin.length() != PIN_LENGTH || !pin.matches("\\d{4}")) {
            throw new RuntimeException("El PIN debe ser de 4 dígitos numéricos");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String role = user.getRole() != null ? user.getRole() : null;
        if (!role.equals("ADMIN_TENANT") && !role.equals("ADMIN") && !role.equals("SUPERVISOR")) {
            throw new RuntimeException("Solo administradores y supervisores pueden tener PIN");
        }

        user.setPinHash(passwordEncoder.encode(pin));
        user.setPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("✅ PIN establecido para usuario: {}", user.getFullName());
    }

    @Transactional
    public boolean validatePin(UUID userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getPinLockedUntil() != null &&
                Instant.now().isBefore(user.getPinLockedUntil())) {
            long minutesLeft = ChronoUnit.MINUTES.between(Instant.now(), user.getPinLockedUntil());
            throw new RuntimeException(String.format(
                    "Cuenta bloqueada por %d minutos. Intenta de nuevo más tarde.", minutesLeft));
        }

        if (user.getPinHash() == null || user.getPinHash().isEmpty()) {
            throw new RuntimeException("Este usuario no tiene PIN configurado. Contacta al administrador.");
        }

        boolean isValid = passwordEncoder.matches(pin, user.getPinHash());

        if (isValid) {
            user.setPinAttempts(0);
            user.setPinLockedUntil(null);
            userRepository.save(user);
            log.info("✅ PIN válido para usuario: {}", user.getFullName());
            return true;
        } else {
            int attempts = user.getPinAttempts() != null ? user.getPinAttempts() + 1 : 1;
            user.setPinAttempts(attempts);

            if (attempts >= MAX_PIN_ATTEMPTS) {
                user.setPinLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
                userRepository.save(user);
                log.warn("⚠️ Usuario {} bloqueado por {} intentos fallidos", user.getFullName(), attempts);
                throw new RuntimeException(String.format(
                        "PIN incorrecto %d veces. Cuenta bloqueada por %d minutos.",
                        MAX_PIN_ATTEMPTS, LOCKOUT_MINUTES));
            }

            userRepository.save(user);
            log.warn("⚠️ PIN incorrecto para usuario {}. Intentos: {}/{}",
                    user.getFullName(), attempts, MAX_PIN_ATTEMPTS);
            throw new RuntimeException(String.format(
                    "PIN incorrecto. Intentos restantes: %d", MAX_PIN_ATTEMPTS - attempts));
        }
    }

    public boolean hasPin(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getPinHash() != null && !user.getPinHash().isEmpty())
                .orElse(false);
    }
}