package org.example.oh_mock.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class Player {
    private int point =0;
    private String nickname;
    private String id;

    private String skinUrl; // [New] 플레이어의 오목알 이미지 URL
    private int stoneType;  // [New] 1: 흑돌(선공), 2: 백돌(후공), 0: 관전

    public Player(String nickname) {
        this.nickname = nickname;
        this.id = UUID.randomUUID().toString();
    }

    public Player(String nickname, String id) {
        this.nickname = nickname;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
