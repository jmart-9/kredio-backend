package com.kredio.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AssignClientRequest {
    private UUID clientId;
    private UUID portfolioId;
}