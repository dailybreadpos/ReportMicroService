package com.dailybread.reportanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime; // Changed from LocalDateTime if it was defined as such
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetSaleResponse {
    private Long id;
    private float cash;
    private float digital;
    private OffsetDateTime date; // CRITICAL: Ensure this is OffsetDateTime
    private List<SaleItem> items;
}
