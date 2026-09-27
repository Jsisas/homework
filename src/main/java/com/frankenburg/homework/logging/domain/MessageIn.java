package com.frankenburg.homework.logging.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"type", "body", "method", "path", "dateTime"})
public class MessageIn {

    private final String type = "messageIn";
    private final String body;
    private final String method;
    private final String path;
    private final String dateTime;

}
