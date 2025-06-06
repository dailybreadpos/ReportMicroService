package com.dailybread.reportanalysis.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleItem {
    private Long itemId;
    private int quantity;

    // ADDED: These fields are present in the JSON response from POSMICROSERVICE
    // They might be null initially from POS, but they need to exist in the DTO
    // for successful deserialization by Feign.
    private String name;
    private String description;
    private String image;
    private double price; // This field was already there, but explicitly listing for clarity
}
