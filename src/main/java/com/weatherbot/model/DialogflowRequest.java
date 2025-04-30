package com.weatherbot.model;

import lombok.Data;
import java.util.Map;

@Data
public class DialogflowRequest {
    private QueryResult queryResult;
    private OriginalDetectIntentRequest originalDetectIntentRequest;
    private String session;

    @Data
    public static class QueryResult {
        private Map<String, Object> parameters;
    }

    @Data
    public static class OriginalDetectIntentRequest {
        private Payload payload;

        @Data
        public static class Payload {
            private User user;
        }

        @Data
        public static class User {
            private String userId;
        }
    }
}
