package org.example.oh_mock.dto;

import lombok.*;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    private String type;      // JOIN, CHAT, STONE, GAME_OVER, EXIT
    private String content;   // 채팅 내용
    private String sender;    // 보낸 사람 닉네임
    private String senderId;  // 보낸 사람 ID

    private String skinUrl;   // 플레이어 돌
    private Integer row;      // 오목판 행
    private Integer col;      // 오목판 열
    private Integer stoneType;// 흑백

    private String winnerName;
    private String winnerSkin;

    public static GameMessage SystemChatMessage(String content) {
        return GameMessage.builder()
                .type("CHAT")
                .sender("SYSTEM")
                .content(content)
                .build();
    }

    public static GameMessage SystemWinnerChatMessage(String content,String winnerName, String winnerSkin) {
        return GameMessage.builder()
                .type("CHAT")
                .sender("SYSTEM")
                .winnerName(winnerName)
                .winnerSkin(winnerSkin)
                .content(content)
                .build();
    }
}