package org.example.oh_mock.service;

import lombok.RequiredArgsConstructor;
import org.example.oh_mock.dto.GameMessage;
import org.example.oh_mock.dto.GameRoom;
import org.example.oh_mock.dto.Player;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    // [입장]
    public synchronized void join(String roomId, GameMessage message){
        GameRoom room = roomService.findRoom(roomId);
        if (room == null) {
            System.out.println("❌ 입장 실패: 방이 존재하지 않음 (" + roomId + ")");
            return;
        }

        Player newPlayer = new Player(message.getSender(), message.getSenderId());
        newPlayer.setSkinUrl(message.getSkinUrl());

        room.assignSeat(newPlayer); // 자리 배정

        System.out.println("✅ 입장: " + message.getSender() + " (Role: " + newPlayer.getStoneType() + ")");
        System.out.println("   현재 방 인원: " + room.getUsers().size() + "명 (Black: " + room.getBlackPlayerId() + ", White: " + room.getWhitePlayerId() + ")");

        // 메시지 전송
        message.setContent(message.getSender() + "님이 입장하셨습니다.");
        message.setStoneType(newPlayer.getStoneType());
        messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", message);
        for (Player p : room.getUsers()) {
            // 방금 입장한 본인(newPlayer) 정보는 위에서 이미 보냈으니 패스
            if (p.getId().equals(newPlayer.getId())) continue;

            // 기존 플레이어(p)의 정보를 담은 메시지 생성
            GameMessage existingPlayerMsg = GameMessage.builder()
                    .type("JOIN") // 프론트에서 JOIN 타입을 받으면 프로필을 갱신하므로 이것을 재활용
                    .sender(p.getNickname())
                    .senderId(p.getId())
                    .stoneType(p.getStoneType()) // 1(흑) 또는 2(백)
                    .skinUrl(p.getSkinUrl())
                    .build();

            // 전송
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", existingPlayerMsg);
        }
    }

    // [착수: 돌 놓기]
    public synchronized void putStone(String roomId, GameMessage message) {
        GameRoom room = roomService.findRoom(roomId);
        if (room == null || !room.isPlaying()) return;

        int row = message.getRow();
        int col = message.getCol();
        int stoneType = message.getStoneType();

        // 1. 턴 체크: 현재 턴과 요청한 돌의 색이 다르면 무시
        if (room.getCurrentTurn() != stoneType) {
            System.out.println("착수 실패: 현재 턴(" + room.getCurrentTurn() + ") != 요청(" + stoneType + ")");
            return;
        }
        // 2. 중복 착수 체크
        if (room.getBoard()[row][col] != 0) return;

        // 3. 착수 처리
        room.getBoard()[row][col] = stoneType;

        // 4. 다음 턴 계산
        int nextTurn = (stoneType == 1) ? 2 : 1;
        room.setCurrentTurn(nextTurn);

        // 5. 메시지 전송 (착수 정보 + 다음 턴 정보)
        message.setType("STONE");
        message.setStoneType(stoneType);
        messagingTemplate.convertAndSend("/topic/" + roomId + "/stone", message);

        // 6. 승리 판정
        if (checkWin(room.getBoard(), row, col, stoneType)) {
            room.setPlaying(false);
            room.setWinnerId(message.getSenderId());

            GameMessage winMsg = GameMessage.SystemWinnerChatMessage(
                    "🎉 " + message.getSender() + "님이 승리하셨습니다!",message.getSender(),message.getSkinUrl());
            winMsg.setType("GAME_OVER");
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", winMsg);
        }
    }

    // [승리 알고리즘: 5목 체크]
    private boolean checkWin(int[][] board, int x, int y, int stone) {
        int[] dx = {1, 0, 1, 1}; // 가로, 세로, 대각선, 역대각선
        int[] dy = {0, 1, 1, -1};

        for (int i = 0; i < 4; i++) {
            int count = 1;
            // 정방향 탐색
            for (int k = 1; k < 5; k++) {
                int nx = x + dx[i] * k;
                int ny = y + dy[i] * k;
                if (nx < 0 || ny < 0 || nx >= 15 || ny >= 15 || board[nx][ny] != stone) break;
                count++;
            }
            // 역방향 탐색
            for (int k = 1; k < 5; k++) {
                int nx = x - dx[i] * k;
                int ny = y - dy[i] * k;
                if (nx < 0 || ny < 0 || nx >= 15 || ny >= 15 || board[nx][ny] != stone) break;
                count++;
            }
            if (count >= 5) return true; // 5개 이상이면 승리
        }
        return false;
    }

    // [게임 시작]
    public void Start(String roomId) {
        GameRoom room = roomService.findRoom(roomId);
        if (room != null) {
            room.resetGame(); // 턴을 1(흑)로 초기화

            GameMessage msg = GameMessage.SystemChatMessage("게임을 시작합니다! 흑돌(⚫)부터 두세요.");
            msg.setType("START");
            // 시작 시 흑돌 차례임을 명시
            msg.setStoneType(1);
            System.out.println("/topic/" + roomId + "/chat"+ new ObjectMapper().writeValueAsString(msg));
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", msg);
        }
    }

    // [퇴장]
    public void exit(String roomId, GameMessage message){
        GameRoom room = roomService.findRoom(roomId);
        if (room != null) {
            Player p = new Player(message.getSender(), message.getSenderId());
            room.removeUser(p); // 흑/백 플레이어였다면 자리 비움 처리됨

            message.setContent(message.getSender() + "님이 퇴장하셨습니다.");
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", message);

            if(room.getUsers().isEmpty()) {
                roomService.deleteRoom(roomId);
            } else if (room.isPlaying() && (room.getBlackPlayerId() == null || room.getWhitePlayerId() == null)) {
                // 게임 중인데 핵심 플레이어가 나가면 게임 중단
                room.setPlaying(false);
                messagingTemplate.convertAndSend("/topic/" + roomId + "/chat",
                        GameMessage.SystemChatMessage("플레이어 퇴장으로 게임이 중단되었습니다."));
            }
        }
    }
}