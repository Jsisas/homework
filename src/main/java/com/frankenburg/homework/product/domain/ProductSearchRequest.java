package com.frankenburg.homework.product.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductSearchRequest {

    @Size(min = 3, max = 10)
    @NotBlank(message = "query is required")
    private String query;

    @Min(1)
    private int page = 1;

}
