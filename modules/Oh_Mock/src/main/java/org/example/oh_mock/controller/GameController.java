package org.example.oh_mock.controller;

import lombok.RequiredArgsConstructor;
import org.example.oh_mock.dto.GameMessage;
import org.example.oh_mock.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @MessageMapping("/{roomId}/join")
    public void join(@DestinationVariable String roomId, GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        // 세션 속성 저장 (디버깅용 로그 추가 가능)
        if(headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("roomId", roomId);
            headerAccessor.getSessionAttributes().put("senderId", message.getSenderId());
            headerAccessor.getSessionAttributes().put("sender", message.getSender());
        }

        gameService.join(roomId, message);
    }

    @MessageMapping("/{roomId}/stone")
    public void putStone(@DestinationVariable String roomId, GameMessage message) {
        gameService.putStone(roomId, message);
    }

    @MessageMapping("/{roomId}/chat")
    @SendTo("/topic/{roomId}/chat")
    public GameMessage chat(@DestinationVariable String roomId, GameMessage message) {
        return message;
    }

    @MessageMapping("/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        gameService.Start(roomId);
    }

    @MessageMapping("/{roomId}/exit")
    public void exit(@DestinationVariable String roomId, GameMessage message) {
        gameService.exit(roomId, message);
    }
}