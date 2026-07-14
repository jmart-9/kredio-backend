package com.kredio.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PortfolioRequest {
    private String name;
    private String description;
    private UUID assignedUserId;
}