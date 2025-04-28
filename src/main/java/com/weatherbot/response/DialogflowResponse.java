package com.weatherbot.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DialogflowResponse {
    private String fulfillmentText;
}
