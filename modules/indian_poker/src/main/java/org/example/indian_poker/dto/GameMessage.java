package org.example.indian_poker.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    private String type;      // JOIN, CHAT, ACTION(게임진행), EXIT
    private String roomId;
    private String sender;
    private String senderId;
    private String content;   // 채팅 메시지

    // 게임마다 달라지는 데이터는 여기에 다 넣음 (좌표, 돌 색깔 등)
    private Map<String, Object> data;
}