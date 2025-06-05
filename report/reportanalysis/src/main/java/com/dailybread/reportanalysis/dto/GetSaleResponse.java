package com.dailybread.reportanalysis.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GetSaleResponse {
    private Long id;
    private float cash;
    private float digital;
    private LocalDateTime date;
    private List<SaleItem> items; // Using the updated SaleItem DTO
}