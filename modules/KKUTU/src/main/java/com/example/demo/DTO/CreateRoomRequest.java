package com.example.demo.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {
    private String roomName;
    private int maxPlayers;
    private int botCount;
    // (향후 'password' 등도 추가 가능)
}