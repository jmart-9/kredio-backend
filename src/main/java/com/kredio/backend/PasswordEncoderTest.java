package com.kredio.backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123"; // Cambia esto por la contraseña que quieras
        String encoded = encoder.encode(password);
        System.out.println("Contraseña encriptada: " + encoded);
    }
}
