package com.dailybread.reportanalysis.dto;

import lombok.Data;

@Data
public class SaleItem {
    private Long itemId;
    private int quantity;
    private String itemName; // To be enriched from Inventory microservice
    private double price; // To be enriched from Inventory microservice
}