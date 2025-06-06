package com.dailybread.reportanalysis.dto;

import lombok.Data;
import java.time.OffsetDateTime; // Changed from LocalDateTime
import java.util.List;

@Data
public class Sales {
    private Long id; // Assuming Sales entity in POS also has an ID
    private float cash;
    private float digital;
    private OffsetDateTime date; // CORRECTED: Changed to OffsetDateTime
    private List<SaleItem> items; // Using the updated SaleItem DTO
}
