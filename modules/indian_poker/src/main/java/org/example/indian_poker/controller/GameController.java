package org.example.indian_poker.controller;


import lombok.RequiredArgsConstructor;
import org.example.indian_poker.dto.GameMessage;
import org.example.indian_poker.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    // 입장
    @MessageMapping("/{roomId}/join")
    public void join(@DestinationVariable String roomId,
                     @Payload GameMessage message,
                     SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("roomId", roomId);
        headerAccessor.getSessionAttributes().put("senderId", message.getSenderId());
        headerAccessor.getSessionAttributes().put("sender", message.getSender()); // 자동 연결 해제를 위함

        gameService.join(roomId, message);
    }


    // 채팅
    @MessageMapping("/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, @Payload GameMessage message) {
        gameService.chat(roomId, message);
    }

    // [통합] 게임 행동 (돌 두기, 그림 그리기, 정답 맞추기 등 모든 게임 로직)
    @MessageMapping("/{roomId}/action")
    public void action(@DestinationVariable String roomId, @Payload GameMessage message) {
        gameService.handleGameAction(roomId, message);
    }

    // 퇴장
    @MessageMapping("/{roomId}/exit")
    public void exit(@DestinationVariable String roomId, @Payload GameMessage message) {
        gameService.exit(roomId, message);
    }
}