package com.dailybread.reportanalysis.dto;

import lombok.Data;

@Data
public class Inventory {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private int stock;
    private String category;
    private String image;
    private boolean disabled;
}