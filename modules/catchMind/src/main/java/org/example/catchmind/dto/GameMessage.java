package org.example.catchmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    private String type;
    private String content; // 채팅 내용 (그림일 경우 비움)
    private String sender;  // 보낸 사람
    private String senderId;
    @JsonProperty("drawer")
    private String drawer;
    private String drawerId;
    @JsonProperty("answer")
    private String answer;
    private List<String> candidates;
    // 그림 좌표 데이터
    private Double x;
    private Double y;
    private String color;
    private Double prevX;
    private Double prevY;

    private List<Player> rankings;
    private Integer currentRound;
    private Integer maxRounds;

    public static GameMessage StartTypeMessageWithContent(GameRoom room, String contents) {
        return GameMessage.builder()
                .type("START")
                .sender("SYSTEM")
                .content(contents)
                .drawer(room.getCurrentDrawer())
                .drawerId(room.getCurrentDrawerId())
                .answer(room.getCurrentAnswer())
                .build();
    }
    public static GameMessage StartTypeMessage(GameRoom room) {
        return StartTypeMessageWithContent(room, "");
    }
    public static GameMessage SystemChatMessage(String contents){
        return GameMessage.builder()
                .type("CHAT")
                .sender("SYSTEM")
                .content(contents)
                .build();
    }
}