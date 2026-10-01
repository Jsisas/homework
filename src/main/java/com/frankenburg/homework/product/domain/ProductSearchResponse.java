package com.frankenburg.homework.product.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductSearchResponse {

    private String title;
    private String description;
    private BigDecimal final_price;
    private int currentPage;
    private int totalPages;

}
