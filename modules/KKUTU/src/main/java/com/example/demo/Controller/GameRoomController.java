package com.example.demo.Controller;
import com.example.demo.listener.WebSocketEventListener;
import com.example.demo.service.GameRoomService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageType;

// --- [!!!] TaskScheduler, Instant, UUID, Map 관련 import 모두 삭제 ---
// (java.util.UUID와 Map은 필요할 수 있으나, 여기선 JoinMessage DTO로 대체)
import java.util.UUID;
import java.util.Map;


@Controller
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;
    private final WebSocketEventListener webSocketEventListener;
    private final SimpMessagingTemplate messagingTemplate;

    // --- TaskScheduler 주입 삭제 (일단 지금은 안씀)---
    // private final TaskScheduler taskScheduler;

    @MessageMapping("/game/{roomId}/word")
    public void submitWord(@DestinationVariable String roomId, @Payload WordMessage message) {
        gameRoomService.handleSubmitFromPlayer(
                roomId,
                message.getWord(),
                message.getUid()
        );
    }

    // ---`joinRoom` 메소드 (클라이언트 UID 사용) ---
    @MessageMapping("/game/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, @Payload JoinMessage message, @Header("simpSessionId") String sessionId) {

        // 클라이언트가 보낸 UID와 닉네임을 사용
        String uid = message.getUid();
        String nickname = message.getNickname();

        String joinResult = gameRoomService.addPlayerToRoom(roomId, uid, nickname);

        if (joinResult.equals("SUCCESS")) {
            // 성공: 세션 등록
            webSocketEventListener.registerSession(sessionId, uid, roomId);
            System.out.println("--- [JOIN SUCCESS] Player " + nickname + " (UID: " + uid + ") joined room " + roomId);

        } else {
            // 실패: 에러 메시지 전송
            String errorMessage = "알 수 없는 오류";
            switch (joinResult) {
                case "NICKNAME_DUPLICATE": errorMessage = "이미 사용 중인 닉네임입니다."; break;
                case "UID_DUPLICATE": errorMessage = "이미 접속 중인 유저입니다. (다른 탭)"; break;
                case "ROOM_FULL": errorMessage = "방이 꽉 찼습니다."; break;
                case "ROOM_NOT_FOUND": errorMessage = "존재하지 않는 방입니다."; break;
            }
            System.err.println("--- [JOIN FAILED] Player " + nickname + " (" + joinResult + ") ---");

            messagingTemplate.convertAndSend(
                    "/user/queue/errors",
                    errorMessage,
                    createHeaders(sessionId)
            );
        }
    }

    // --- `createHeaders` 헬퍼 ---
    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }

    @MessageMapping("/game/{roomId}/forfeit")
    public void forfeitTurn(@DestinationVariable String roomId, @Payload ForfeitMessage message) {
        gameRoomService.passTurn(roomId, message.getUid());
    }

    // --- DTO 클래스 (JoinMessage 수정) ---
    @Getter @Setter private static class ForfeitMessage { private String uid; }

    @Getter @Setter
    private static class JoinMessage {
        private String uid;
        private String nickname;
    }

    @Getter @Setter private static class WordMessage { private String word; private String uid; }
}