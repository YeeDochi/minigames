package org.example.catchmind.controller;

import lombok.RequiredArgsConstructor;
import org.example.catchmind.dto.GameMessage;
import org.example.catchmind.dto.GameRoom;
import org.example.catchmind.service.GameService;
import org.example.catchmind.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameController {

    private final RoomService roomService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/{roomId}/join")
    public void join(@DestinationVariable String roomId, GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("roomId", roomId);
        headerAccessor.getSessionAttributes().put("senderId", message.getSenderId());
        headerAccessor.getSessionAttributes().put("sender", message.getSender());
        gameService.join(roomId, message);
    }

    @MessageMapping("/{roomId}/exit")
    public void exit(@DestinationVariable String roomId, GameMessage message) {
        gameService.exit(roomId, message);
    }

    @MessageMapping("/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        gameService.Start(roomId);
    }

    @MessageMapping("/{roomId}/draw")
    @SendTo("/topic/{roomId}/draw")
    public GameMessage broadcastDraw(@DestinationVariable String roomId, GameMessage message) {
        return message;
    }

    @MessageMapping("/{roomId}/chat")
    public synchronized void chat(@DestinationVariable String roomId, GameMessage message) {
        GameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        if (room.getCurrentDrawerId() != null &&
                room.getCurrentDrawerId().equals(message.getSenderId())) {
            return;
        }

        // [정답 맞혔을 때]
        if (room.getCurrentAnswer() != null &&
                message.getContent().trim().equals(room.getCurrentAnswer())) {
            room.addScore(message.getSenderId(), 10);
            room.setCurrentRound(room.getCurrentRound() + 1);
            GameMessage correctMsg = GameMessage.SystemChatMessage(
                    "🎉 " + message.getSender() + "님 정답! (+10점)");
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", correctMsg);

            gameService.startNextTurn(roomId, message.getSenderId());

        } else {
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", message);
        }
    }

    @MessageMapping("/{roomId}/choose")
    public void chooseWord(@DestinationVariable String roomId, GameMessage message) {
        GameRoom room = roomService.findRoom(roomId);
        if(room != null && message.getSenderId().equals(room.getCurrentDrawerId())) {
            gameService.chooseWord(roomId, message.getContent());
        }
    }
    @MessageMapping("/{roomId}/input")
    public void inputWord(@DestinationVariable String roomId, GameMessage message){
        GameRoom room = roomService.findRoom(roomId);
        if(room != null && message.getSenderId().equals(room.getCurrentDrawerId())) {
            gameService.inputWord(roomId, message.getContent());
        }
    }
}