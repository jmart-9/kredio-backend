package com.kredio.backend.engine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AmortizationConfig {

    @Bean
    public Map<AmortizationMethod, AmortizationEngine> amortizationEngines(
            FrenchAmortizationCalculator french,
            FlatAmortizationCalculator flat,
            GermanAmortizationCalculator german) {

        return Map.of(
                AmortizationMethod.FRENCH, french,
                AmortizationMethod.FLAT, flat,
                AmortizationMethod.GERMAN, german
        );
    }
}