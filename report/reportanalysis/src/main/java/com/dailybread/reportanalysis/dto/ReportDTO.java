package com.dailybread.reportanalysis.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportDTO {
    private String itemName;
    private int quantitySold;
    private double revenue;
    private int remainingStock;
    private String category;
}
