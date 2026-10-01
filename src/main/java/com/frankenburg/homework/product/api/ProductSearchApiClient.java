package com.frankenburg.homework.product.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/products")
public interface ProductSearchApiClient {

    int PAGE_SIZE = 2;

    @GetExchange("/search?limit=" + PAGE_SIZE)
    ProductSearchResult search(@RequestParam("q") String query, @RequestParam("skip") long skip);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductSearchResult(List<DummyProduct> products, int total) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DummyProduct(String title, String description, double price, double discountPercentage) {
        public BigDecimal getProductDiscountedPrice() {
            return BigDecimal.valueOf(this.price())
                    .multiply(BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(discountPercentage())))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

}
