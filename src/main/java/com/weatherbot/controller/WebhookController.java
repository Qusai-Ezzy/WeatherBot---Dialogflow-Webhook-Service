package com.weatherbot.controller;

import com.weatherbot.model.DialogflowRequest;
import com.weatherbot.response.DialogflowResponse;
import com.weatherbot.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WeatherService weatherService;

    @PostMapping
    public DialogflowResponse handleWebhook(@RequestBody DialogflowRequest request) {
        return weatherService.processWeatherRequest(request);
    }
}
