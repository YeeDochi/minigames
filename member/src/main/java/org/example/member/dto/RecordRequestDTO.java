package org.example.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordRequestDTO {
    private String username; // 또는 userId
    private String gameType;
    private int score;
    private boolean isScore = false; //접수형 게임이면 true 승수면 false
}
