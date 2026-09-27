package com.frankenburg.homework.product;

import java.util.List;

import com.frankenburg.homework.product.api.ProductSearchApiClient;
import com.frankenburg.homework.product.domain.ProductSearchRequest;
import com.frankenburg.homework.product.domain.ProductSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchApiClient apiClient;

    public List<ProductSearchResponse> search(ProductSearchRequest productSearchRequest) {
        return apiClient.search(productSearchRequest.getQuery(), pagesToSkip(productSearchRequest.getPage()))
                .products()
                .stream()
                .map(dummyProduct -> new ProductSearchResponse(dummyProduct.title(), dummyProduct.description(), dummyProduct.getProductDiscountedPrice()))
                .toList();
    }

    private static long pagesToSkip(int page) {
        return (page - 1L) * ProductSearchApiClient.PAGE_SIZE;
    }

}
