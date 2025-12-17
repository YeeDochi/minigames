package com.example.demo.bots;

import com.example.demo.service.GameRoomService;
import com.example.demo.Event.WordValidationRequestEvent;
import com.example.demo.service.KoreanApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DictionaryBotService {
    private final KoreanApiService koreanApiService;
    private final GameRoomService gameRoomService; // 결과 콜백용

    @Async //별도 스레드 풀에서 비동기 실행
    @EventListener // WordValidationRequestEvent 이벤트가 발생하면 이 메서드 실행
    public void onWordValidationRequest(WordValidationRequestEvent event) {
        

        // 1. 국어원 API 호출
        Map<String, Object> validationResult = koreanApiService.validateWord(event.getWord());

        // 2. Map에서 값 추출
        boolean isValid = (Boolean) validationResult.getOrDefault("isValid", false);
        String definition = (String) validationResult.get("definition"); // 실패 시 null

        // 3. GameRoomService 콜백 호출 시 definition 추가
        gameRoomService.processValidationResult(
                event.getRoomId(),
                event.getUserId(),
                event.getWord(),
                isValid,
                definition // [!!!] 추출된 뜻 전달
        );
    }
}
