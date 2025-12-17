package com.example.demo.listener; // 패키지 확인

import com.example.demo.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로깅 라이브러리 (선택 사항)
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final GameRoomService gameRoomService;

    // (세션ID -> 유저UID)
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    // (세션ID -> 방ID)
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    // (연결 리스너는 주석 처리됨 - 현 로직에선 불필요)

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String uid = sessionUserMap.remove(sessionId);
        String roomId = sessionRoomMap.remove(sessionId);

        if (uid != null && roomId != null) {
            log.info("WebSocket Disconnected: User UID '{}' from Room '{}'", uid, roomId);
            //  uid로 연결 해제 처리 위임
            gameRoomService.handlePlayerDisconnect(roomId, uid);
        } else if (uid != null) {
            log.info("WebSocket Disconnected: User UID '{}' (before joining room)", uid);
        } else {
            log.warn("WebSocket Disconnected: SessionId={} (User or Room info not found)", sessionId);
        }
    }

    // --- `registerSession` (uid) ---
    /**
     * 플레이어가 방에 성공적으로 Join했을 때 호출되어 세션 정보를 저장합니다.
     */
    public void registerSession(String sessionId, String uid, String roomId) {
        if (sessionId == null || uid == null || roomId == null) return;
        sessionUserMap.put(sessionId, uid);
        sessionRoomMap.put(sessionId, roomId);
        log.info("Session Registered: SessionId={}, User UID={}, Room={}", sessionId, uid, roomId);
    }
}