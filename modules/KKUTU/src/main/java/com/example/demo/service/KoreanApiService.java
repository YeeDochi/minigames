package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@Service
//@RequiredArgsConstructor
public class KoreanApiService {

    private final WebClient koreanApiWebClient; // 2단계에서 만든 WebClient 주입
    private final ObjectMapper objectMapper;

    public KoreanApiService(WebClient koreanApiWebClient, ObjectMapper objectMapper) {
        this.koreanApiWebClient = koreanApiWebClient;
        this.objectMapper = objectMapper;
    }
    @Value("${api.key.korean}")
    private String apiKey;

    public Map<String, Object> validateWord(String word) {
        Map<String, Object> result = new HashMap<>();
        result.put("isValid", false); // 기본값 false
        result.put("definition", null); // 기본값 null

        try {
            String rawResponse = koreanApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search.do")
                            .queryParam("key", apiKey)
                            .queryParam("q", word)
                            .queryParam("req_type", "json")
                            .queryParam("method", "exact") // 정확히 일치하는 단어만
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 결과 대기

            System.out.println("<<< API Raw Response for [" + word + "]: " + rawResponse); // 디버깅 로그

            if (rawResponse == null || rawResponse.isEmpty()) {
                System.err.println("!!! API FAIL (Response Null/Empty) for word: [" + word + "]");
                // isValid는 이미 false
            } else {
                JsonNode response = objectMapper.readTree(rawResponse);
                if (response != null && response.has("channel") &&
                        response.get("channel").has("total") &&
                        response.get("channel").get("total").asInt() > 0) {

                    String finalDefinition = "뜻 정보 없음"; // 기본값

                    try {
                        JsonNode itemNode = response.path("channel").path("item");
                        if (itemNode.isArray() && itemNode.size() > 0) {
                            JsonNode senseNode = itemNode.get(0).path("sense");

                            // sense가 배열인지 객체인지 확인
                            if (senseNode.isArray() && senseNode.size() > 0) {
                                // 배열이면 첫 번째 요소 사용
                                finalDefinition = senseNode.get(0).path("definition").asText("뜻 정보 없음");
                            } else if (senseNode.isObject()) {
                                // 객체면 바로 사용
                                finalDefinition = senseNode.path("definition").asText("뜻 정보 없음");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("!!! API Definition Parse Error for [" + word + "]: " + e.getMessage());
                        // finalDefinition은 기본값 "뜻 정보 없음" 유지
                    }

                    System.out.println("--- API SUCCESS for word: [" + word + "], Definition: [" + finalDefinition + "]");
                    result.put("isValid", true);
                    result.put("definition", finalDefinition); //  추출된 뜻 저장

                } else {
                    System.err.println("!!! API FAIL (Total <= 0 or Invalid JSON) for word: [" + word + "]");
                    // isValid는 이미 false
                }
            }
        } catch (Exception e) {
            System.err.println("!!! API EXCEPTION for word: [" + word + "]: " + e.getMessage());
            // isValid는 이미 false
        }

        System.out.println("--- KoreanApiService.validateWord returning: " + result + " for word: [" + word + "]");
        return result; //  Map 반환
    }
}
