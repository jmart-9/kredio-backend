package com.kredio.backend.service;

import com.kredio.backend.entity.Country;
import com.kredio.backend.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    public Country getCountryById(UUID id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("País no encontrado"));
    }

    public Country getCountryByCode(String code) {
        return countryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("País no encontrado"));
    }
}