package org.example.catchmind.dto;

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

    public Player(String nickname) {
        this.nickname = nickname;
        this.id = UUID.randomUUID().toString();
    }

    public Player(String nickname, String id) {
        this.nickname = nickname;
        this.id = id;
    }

    public void addPoint(int point){
        this.point += point;
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
