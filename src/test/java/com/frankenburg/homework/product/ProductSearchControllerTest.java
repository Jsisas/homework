package com.frankenburg.homework.product;

import java.util.List;

import com.frankenburg.homework.product.api.ProductSearchApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSearchControllerTest {

    private static final String PATH = "/products/search";
    private static final String JSON_REQUEST = "{\"query\":\"phone\"}";
    private static final String XML_REQUEST = "<request><query>phone</query></request>";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductSearchApiClient apiClient;

    @BeforeEach
    void stubApi() {
        given(apiClient.search("phone", 0)).willReturn(new ProductSearchApiClient.ProductSearchResult(List.of(
                new ProductSearchApiClient.DummyProduct("Apple AirPods Max Silver", "Headphones", 549.99, 13.67),
                new ProductSearchApiClient.DummyProduct("Apple iPhone Charger", "Charger", 19.99, 18.52))));
    }

    @Test
    void jsonSearchDefaultsToFirstPage() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"phone\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Apple AirPods Max Silver"))
                .andExpect(jsonPath("$[0].description").value("Headphones"))
                .andExpect(jsonPath("$[0].final_price").value(474.81))
                .andExpect(jsonPath("$[1].final_price").value(16.29));
    }

    @Test
    void jsonSearchSecondPage() throws Exception {
        given(apiClient.search("phone", 2)).willReturn(new ProductSearchApiClient.ProductSearchResult(List.of(
                new ProductSearchApiClient.DummyProduct("Apple MagSafe Battery Pack", "Battery", 99.99, 17.17))));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"phone\",\"page\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Apple MagSafe Battery Pack"));
    }

    @Test
    void finalPriceKeepsTrailingZeros() throws Exception {
        given(apiClient.search("round", 0)).willReturn(new ProductSearchApiClient.ProductSearchResult(List.of(
                new ProductSearchApiClient.DummyProduct("Half off", "Discounted", 20, 50))));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"round\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"title\":\"Half off\",\"description\":\"Discounted\",\"final_price\":10.00}]"));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).accept(APPLICATION_XML).content("{\"query\":\"round\"}"))
                .andExpect(status().isOk())
                .andExpect(xpath("/List/item[1]/final_price").string("10.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"query\":\"\"}",
            "{\"query\":\"   \"}",
            "{\"query\":\"ab\"}",
            "{\"query\":\"elevenchars\"}",
            "{\"query\":\"phone\",\"page\":0}",
            "{\"query\":\"phone\",\"page\":-1}"
    })
    void invalidRequestReturnsBadRequest(String body) throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void xmlRequestWithXmlAcceptReturnsXml() throws Exception {
        mvc.perform(post(PATH).contentType(APPLICATION_XML).accept(APPLICATION_XML).content(XML_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_XML))
                .andExpect(xpath("/List/item[1]/title").string("Apple AirPods Max Silver"))
                .andExpect(xpath("/List/item[1]/final_price").number(474.81));
    }

    @Test
    void xmlRequestWithJsonAcceptReturnsJson() throws Exception {
        mvc.perform(post(PATH).contentType(APPLICATION_XML).accept(APPLICATION_JSON).content(XML_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$[0].final_price").value(474.81));
    }

    @Test
    void jsonRequestWithXmlAcceptReturnsXml() throws Exception {
        mvc.perform(post(PATH).contentType(APPLICATION_JSON).accept(APPLICATION_XML).content(JSON_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_XML))
                .andExpect(xpath("/List/item[1]/title").string("Apple AirPods Max Silver"));
    }

    @Test
    void validationErrorIsReturnedInXmlWhenXmlAccepted() throws Exception {
        mvc.perform(post(PATH).contentType(APPLICATION_XML).accept(APPLICATION_XML)
                        .content("<request><query>ab</query></request>"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_XML))
                .andExpect(xpath("/*/code").number(400.0))
                .andExpect(xpath("/*/message").string("query: size must be between 3 and 10"));
    }

    @Test
    void malformedXmlIsRejectedInXml() throws Exception {
        mvc.perform(post(PATH).contentType(APPLICATION_XML).accept(APPLICATION_XML).content("<request><query>"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_XML))
                .andExpect(xpath("/*/message").string("Malformed request body"));
    }

    @Test
    void apiErrorIsReturnedInXmlWhenXmlAccepted() throws Exception {
        given(apiClient.search("phone", 0)).willThrow(new ResourceAccessException("Connection refused"));

        mvc.perform(post(PATH).contentType(APPLICATION_JSON).accept(APPLICATION_XML).content(JSON_REQUEST))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_XML))
                .andExpect(xpath("/*/code").number(502.0))
                .andExpect(xpath("/*/message").string("Product API is unreachable: Connection refused"));
    }

    @Test
    void apiErrorIsReturnedInJsonWhenJsonAccepted() throws Exception {
        given(apiClient.search("phone", 0)).willThrow(new ResourceAccessException("Connection refused"));

        mvc.perform(post(PATH).contentType(APPLICATION_XML).accept(APPLICATION_JSON).content(XML_REQUEST))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(502));
    }
}