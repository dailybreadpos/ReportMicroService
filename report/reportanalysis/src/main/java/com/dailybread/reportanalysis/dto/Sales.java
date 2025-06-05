package com.dailybread.reportanalysis.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Sales {
    private Long id; // Assuming Sales entity in POS also has an ID
    private float cash;
    private float digital;
    private LocalDateTime date;
    private List<SaleItem> items; // Using the updated SaleItem DTO
}