package com.frankenburg.homework.product.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductSearchApiErrorResponse {

    private int code;
    private String message;

}
