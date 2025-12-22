package org.example.indian_poker.service;

import lombok.*;
import org.example.indian_poker.dto.BaseGameRoom;
import org.example.indian_poker.dto.GameMessage;
import org.example.indian_poker.dto.Player;
import org.example.indian_poker.service.RoomService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    // 입장 처리
    public void join(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        room.enterUser(new Player(message.getSender(), message.getSenderId()));

        message.setType("JOIN");
        message.setContent(message.getSender() + "님이 입장하셨습니다.");
        broadcast(roomId, message);

        // ... (기존 broadcast 코드) ...

        // [Tip] 실제 구현 시 주석 해제: 기존 유저 정보를 신규 유저에게 동기화
//        for (Player p : room.getUsers().values()) {
//            if (p.getId().equals(message.getSenderId())) continue; // 나 자신 제외
//
//            GameMessage syncMsg = GameMessage.builder()
//                    .type("JOIN")
//                    .sender(p.getNickname())
//                    .senderId(p.getId())
//                    // Player의 attributes나 skinUrl을 data에 담아서 전송
//                    .data(Map.of("semple", "semple"))
//                    .build();
//
//            messagingTemplate.convertAndSend("/topic/" + roomId, syncMsg);
//        }
        GameMessage syncMsg = new GameMessage();
        syncMsg.setType("SYNC");
        syncMsg.setRoomId(roomId);
        syncMsg.setSender("SYSTEM");
        syncMsg.setData(room.getGameSnapshot());
        broadcast(roomId, syncMsg);
    }

    // 게임 행동 처리 (핵심)
    public void handleGameAction(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        GameMessage result = room.handleAction(message);

        if (result != null) {
            broadcast(roomId, result);
        }
    }

    public void chat(String roomId, GameMessage message) {
        broadcast(roomId, message);
    }

    public void exit(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room != null) {
            room.exitUser(message.getSenderId());
            if (room.getUsers().isEmpty()) {
                roomService.deleteRoom(roomId);
            } else {
                broadcast(roomId, message);
            }
        }
    }

    private void broadcast(String roomId, GameMessage message) {
        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }
}