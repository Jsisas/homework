package com.frankenburg.homework.product.api;

import java.util.stream.Collectors;

import com.frankenburg.homework.product.ProductSearchController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
@RestControllerAdvice(assignableTypes = ProductSearchController.class)
public class ProductSearchExceptionHandler {

    private final JsonMapper jsonMapper;

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ProductSearchApiErrorResponse> handleApiErrorResponse(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status)
                .body(new ProductSearchApiErrorResponse(status, upstreamMessage(ex)));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ProductSearchApiErrorResponse> handleApiUnreachable(ResourceAccessException ex) {
        int status = HttpStatus.BAD_GATEWAY.value();
        return ResponseEntity.status(status)
                .body(new ProductSearchApiErrorResponse(status, "Product API is unreachable: " + ex.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ProductSearchApiErrorResponse> handleApiFailure(RestClientException ex) {
        int status = HttpStatus.BAD_GATEWAY.value();
        return ResponseEntity.status(status)
                .body(new ProductSearchApiErrorResponse(status, "Product API call failed: " + ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProductSearchApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProductSearchApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return badRequest("Malformed request body");
    }

    private static ResponseEntity<ProductSearchApiErrorResponse> badRequest(String message) {
        int status = HttpStatus.BAD_REQUEST.value();
        return ResponseEntity.status(status).body(new ProductSearchApiErrorResponse(status, message));
    }

    private String upstreamMessage(RestClientResponseException ex) {
        try {
            JsonNode message = jsonMapper.readTree(ex.getResponseBodyAsString()).get("message");
            if (message != null && message.isString()) {
                return message.asString();
            }
        } catch (JacksonException ignored) {
            // Fall back to the status text
        }
        return ex.getStatusText();
    }

}
