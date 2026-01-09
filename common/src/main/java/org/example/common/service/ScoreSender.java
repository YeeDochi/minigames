package org.example.common.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.RecordRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

@Service
@RequiredArgsConstructor
public class ScoreSender {

    private final RestTemplate restTemplate;


    @Value("${member.api.url}")
    private String MEMBER_API_URL;

    public void sendScore(String username, String gameType, int score, boolean isScore) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-SERVER-KEY", "MY_SUPER_SECRET_KEY");

            RecordRequestDTO requestDto = new RecordRequestDTO(username, gameType, score, isScore);

            HttpEntity<RecordRequestDTO> request = new HttpEntity<>(requestDto, headers);

            restTemplate.postForEntity(MEMBER_API_URL, request, String.class);
            System.out.println("✅ [" + gameType + "] 점수 전송 성공: " + username + " (" + score + "점)");

        } catch (Exception e) {
            System.err.println("❌ 점수 전송 실패: " + e.getMessage());
        }
    }
}