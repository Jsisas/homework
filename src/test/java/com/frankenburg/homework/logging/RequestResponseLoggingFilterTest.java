package com.frankenburg.homework.logging;

import java.util.List;

import com.frankenburg.homework.product.api.ProductSearchApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RequestResponseLoggingFilterTest {

    private static final String PATH = "/products/search";
    private static final String BODY = "{\"query\":\"phone\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ProductSearchApiClient apiClient;

    @Test
    void successfulExchangeIsLoggedAsMessageInThenMessageOut(CapturedOutput output) throws Exception {
        given(apiClient.search(anyString(), anyLong())).willReturn(new ProductSearchApiClient.ProductSearchResult(List.of(
                new ProductSearchApiClient.DummyProduct("Apple AirPods Max Silver", "Headphones", 549.99, 13.67))));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY));

        List<JsonNode> entries = logEntries(output);
        assertThat(entries).hasSize(2);

        JsonNode in = entries.get(0);
        assertThat(in.get("type").asString()).isEqualTo("messageIn");
        assertThat(in.get("body").asString()).isEqualTo(BODY);
        assertThat(in.get("method").asString()).isEqualTo("POST");
        assertThat(in.get("path").asString()).isEqualTo("http://localhost" + PATH);
        assertThat(in.get("dateTime").asString()).isNotBlank();

        JsonNode out = entries.get(1);
        assertThat(out.get("type").asString()).isEqualTo("messageOut");
        assertThat(out.get("body").asString()).contains("\"title\":\"Apple AirPods Max Silver\"", "\"final_price\":474.81");
        assertThat(out.get("dateTime").asString()).isNotBlank();
        assertThat(out.has("fault")).isFalse();
    }

    @Test
    void handledApiErrorIsLoggedWithErrorBodyAndFault(CapturedOutput output) throws Exception {
        given(apiClient.search(anyString(), anyLong())).willThrow(new ResourceAccessException("Connection refused"));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY));

        JsonNode out = logEntries(output).get(1);
        assertThat(out.get("body").asString()).contains("\"code\":502");
        assertThat(out.get("fault").asString())
                .startsWith("org.springframework.web.client.ResourceAccessException: Connection refused")
                .contains("\tat ");
    }

    @Test
    void validationErrorIsLoggedWithFault(CapturedOutput output) throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"ab\"}"));

        List<JsonNode> entries = logEntries(output);
        assertThat(entries.get(0).get("body").asString()).isEqualTo("{\"query\":\"ab\"}");
        assertThat(entries.get(1).get("body").asString())
                .isEqualTo("{\"code\":400,\"message\":\"query: size must be between 3 and 10\"}");
        assertThat(entries.get(1).get("fault").asString())
                .startsWith("org.springframework.web.bind.MethodArgumentNotValidException");
    }

    private List<JsonNode> logEntries(CapturedOutput output) {
        return output.getOut().lines()
                .filter(line -> line.contains(RequestResponseLoggingFilter.class.getSimpleName() + " ")
                        && line.contains("{\"type\":\"message"))
                .map(line -> line.substring(line.indexOf("{\"type\":\"message")))
                .map(jsonMapper::readTree)
                .toList();
    }

}
