package com.dailybread.reportanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySalesData {
    private String week; // e.g., "W1", "W2"
    private double total;
}
