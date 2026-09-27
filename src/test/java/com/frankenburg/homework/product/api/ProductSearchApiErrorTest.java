package com.frankenburg.homework.product.api;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSearchApiErrorTest {

    private static final String PATH = "/products/search";
    private static final String BODY = "{\"query\":\"phone\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductSearchApiClient apiClient;

    @Test
    void upstreamClientErrorIsPassedThroughWithItsMessage() throws Exception {
        given(apiClient.search(anyString(), anyLong())).willThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "{\"message\":\"Invalid 'limit' - should be a positive number\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid 'limit' - should be a positive number"));
    }

    @Test
    void upstreamServerErrorWithNonJsonBodyFallsBackToStatusText() throws Exception {
        given(apiClient.search(anyString(), anyLong())).willThrow(HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY,
                "<html>down</html>".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("Service Unavailable"));
    }

    @Test
    void unreachableApiReturnsBadGateway() throws Exception {
        given(apiClient.search(anyString(), anyLong())).willThrow(new ResourceAccessException("Connection refused"));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("Product API is unreachable: Connection refused"));
    }

    @Test
    void undecodableApiResponseReturnsBadGateway() throws Exception {
        given(apiClient.search(anyString(), anyLong())).willThrow(new RestClientException("Could not extract response"));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("Product API call failed: Could not extract response"));
    }

}
