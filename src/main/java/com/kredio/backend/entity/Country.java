package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 3, unique = true)
    private String code; // GT, SV, HN, NI, CR, PA

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode; // GTQ, USD, HNL, NIO, CRC

    @Column(name = "currency_symbol", nullable = false, length = 5)
    private String currencySymbol; // Q, $, L, C$, ₡

    @Column(name = "max_legal_interest_rate", precision = 5, scale = 2)
    private java.math.BigDecimal maxLegalInterestRate;

    @Column(name = "date_format", length = 10)
    private String dateFormat;

    @Column(name = "number_format", length = 10)
    private String numberFormat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "id_document_types", columnDefinition = "jsonb")
    private String idDocumentTypes; // JSON array

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "regulatory_requirements", columnDefinition = "jsonb")
    private String regulatoryRequirements; // JSON object

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}