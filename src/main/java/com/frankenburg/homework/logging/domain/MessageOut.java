package com.frankenburg.homework.logging.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "body", "dateTime", "fault"})
public class MessageOut {

    private final String type = "messageOut";
    private final String body;
    private final String dateTime;
    private final String fault;

}
