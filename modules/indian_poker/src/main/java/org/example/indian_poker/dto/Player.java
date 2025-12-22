package org.example.indian_poker.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class Player {
    private String id;        // 고유 ID (UUID)
    private String nickname;  // 닉네임

    private Map<String, Object> attributes = new HashMap<>();

    public Player(String nickname, String id) {
        this.nickname = nickname;
        this.id = id;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    // 1. 정수값 꺼내기 (예: 오목 돌 종류, 점수 등)
    public int getInt(String key) {
        Object val = attributes.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue(); // 안전하게 int로 변환
        }
        return 0; // 값이 없거나 숫자가 아니면 0 반환 (Null Pointer Exception 방지)
    }

    // 2. 문자열 꺼내기 (예: 팀 이름, 역할 등)
    public String getString(String key) {
        Object val = attributes.get(key);
        return val != null ? val.toString() : "";
    }

    // 3. 불리언 꺼내기 (예: 준비 완료 여부, 술래 여부)
    public boolean getBoolean(String key) {
        Object val = attributes.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        // 문자열 "true"로 들어왔을 경우도 대비
        return val != null && Boolean.parseBoolean(val.toString());
    }

    // 4. 값 넣기 (단축 메서드)
    public void set(String key, Object value) {
        attributes.put(key, value);
    }
}