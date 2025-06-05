package com.dailybread.reportanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySalesData {
    private String day; // e.g., "MONDAY", "TUESDAY"
    private double total;
}
