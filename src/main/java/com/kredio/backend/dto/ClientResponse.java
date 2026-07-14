package com.kredio.backend.dto;

import java.util.UUID;

public record ClientResponse(
        UUID id,
        String fullName,
        String dpiOrId,
        String phone,
        String address,
        UUID portfolioId
) {
}