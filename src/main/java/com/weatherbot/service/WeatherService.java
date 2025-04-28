package com.weatherbot.service;

import com.weatherbot.model.DialogflowRequest;
import com.weatherbot.response.DialogflowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${weather.api.key}")
    private String apiKey;

    public DialogflowResponse processWeatherRequest(DialogflowRequest request) {
        Map<String, Object> parameters = request.getQueryResult().getParameters();

        String city = (String) parameters.get("geo-city");
        String dateString = null;

        // Handle both 'date' and 'date-time'
        if (parameters.get("date-time") != null) {
            dateString = parameters.get("date-time").toString();
        } else if (parameters.get("date") != null) {
            dateString = parameters.get("date").toString();
        }

        if (city == null || city.isEmpty()) {
            return new DialogflowResponse("Please provide a city name.");
        }

        if (dateString == null || dateString.isEmpty()) {
            // No date provided -> fetch current weather
            return new DialogflowResponse(fetchCurrentWeather(city));
        } else {
            // Date provided -> fetch forecast
            return new DialogflowResponse(fetchForecastWeather(city, dateString));
        }
    }

    private String fetchCurrentWeather(String city) {
        try {
            String weatherUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=metric&appid=" + apiKey;
            Map<String, Object> weatherResponse = restTemplate.getForObject(weatherUrl, Map.class);

            if (weatherResponse == null) return "Weather info not found.";

            Number tempNumber = (Number) ((Map<String, Object>) weatherResponse.get("main")).get("temp");
            double temp = tempNumber.doubleValue();
            Map<String, Object> weather = (Map<String, Object>) ((java.util.List<?>) weatherResponse.get("weather")).get(0);
            String description = (String) weather.get("description");

            return "Current weather in " + city + ": " + temp + "°C, " + description + ".";
        } catch (Exception e) {
            return "Error fetching weather: " + e.getMessage();
        }
    }

    private String fetchForecastWeather(String city, String dateString) {
        try {
            String forecastUrl = "http://api.openweathermap.org/data/2.5/forecast?q=" + city + "&units=metric&appid=" + apiKey;
            Map<String, Object> forecastResponse = restTemplate.getForObject(forecastUrl, Map.class);

            if (forecastResponse == null) return "Forecast info not found.";

            var list = (java.util.List<Map<String, Object>>) forecastResponse.get("list");

            // Extract only the date part if full datetime received
            if (dateString.contains("T")) {
                dateString = dateString.split("T")[0];
            }

            LocalDate targetDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
            LocalDate today = LocalDate.now(ZoneId.systemDefault());

            if (targetDate.isAfter(today.plusDays(5))) {
                return "Sorry, I can only provide weather forecasts up to 5 days from today.";
            }

            double totalTemp = 0;
            int tempCount = 0;
            double totalWindSpeed = 0;
            double totalCloudCoverage = 0;
            String condition = "";

            // Iterate over the forecast data and aggregate the values
            for (Map<String, Object> item : list) {
                String dtTxt = (String) item.get("dt_txt"); // e.g., "2022-08-30 15:00:00"
                LocalDate itemDate = LocalDate.parse(dtTxt.substring(0, 10));

                if (itemDate.equals(targetDate)) {
                    // Aggregate temperature data
                    Number tempNumber = (Number) ((Map<String, Object>) item.get("main")).get("temp");
                    double temp = tempNumber.doubleValue();
                    totalTemp += temp;
                    tempCount++;

                    // Aggregate wind speed
                    Map<String, Object> wind = (Map<String, Object>) item.get("wind");
                    Number windSpeed = (Number) wind.get("speed");
                    double windSpeedValue = windSpeed.doubleValue();
                    totalWindSpeed += windSpeedValue;

                    // Aggregate cloud coverage
                    Map<String, Object> clouds = (Map<String, Object>) item.get("clouds");
                    Number cloudCoverage = (Number) clouds.get("all");
                    double cloudCoverageValue = cloudCoverage.doubleValue();
                    totalCloudCoverage += cloudCoverageValue;

                    // Pick a condition from the forecast (you could choose the most frequent condition or the first one)
                    Map<String, Object> weather = (Map<String, Object>) ((java.util.List<?>) item.get("weather")).get(0);
                    condition = (String) weather.get("description");
                }
            }

            if (tempCount > 0) {
                double avgTemp = totalTemp / tempCount;
                double avgWindSpeed = totalWindSpeed / tempCount;
                double avgCloudCoverage = totalCloudCoverage / tempCount;

                return String.format("Forecasted weather in %s on %s:\nTemperature: %.2f°C\nCondition: %s\nCloud Coverage: %.0f%%\nWind Speed: %.2f m/s\n",
                        city, targetDate, avgTemp, condition, avgCloudCoverage, avgWindSpeed);
            } else {
                return "No forecast available for " + city + " on " + targetDate + ".";
            }
        } catch (Exception e) {
            return "Error fetching forecast: " + e.getMessage();
        }
    }

}
