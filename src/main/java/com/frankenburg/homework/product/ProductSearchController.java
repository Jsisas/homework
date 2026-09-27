package com.frankenburg.homework.product;

import java.util.List;

import com.frankenburg.homework.product.domain.ProductSearchRequest;
import com.frankenburg.homework.product.domain.ProductSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService service;

    @PostMapping(
            path = "/products/search",
            consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE},
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE}
    )
    public List<ProductSearchResponse> search(@Valid @RequestBody ProductSearchRequest request) {
        return service.search(request);
    }

}
