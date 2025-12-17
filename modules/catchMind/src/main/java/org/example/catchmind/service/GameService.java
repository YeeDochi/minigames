package org.example.catchmind.service;

import lombok.RequiredArgsConstructor;
import org.example.catchmind.dto.GameMessage;
import org.example.catchmind.dto.GameRoom;
import org.example.catchmind.dto.Player;
import org.example.catchmind.repo.WordsEntity;
import org.example.catchmind.repo.WordsRepo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WordsRepo wordsRepo;

    public void join(String roomId, GameMessage message){
        GameRoom room = roomService.findRoom(roomId);

        if (room != null) {
            // [수정] 게임 중이면 강퇴 메시지 전송
            if (room.isPlaying()) {
                GameMessage kickMsg = GameMessage.builder()
                        .type("KICK")
                        .sender("SYSTEM")
                        .senderId(message.getSenderId()) // 쫓겨날 사람 ID
                        .content("이미 게임이 진행 중입니다.")
                        .build();
                // 해당 방의 모두에게 보내지만, 프론트에서 본인 ID만 체크해서 나감
                messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", kickMsg);
                return;
            }

            room.addUser(new Player(message.getSender(),message.getSenderId()));

            System.out.println("방(" + roomId + ") 입장: " + message.getSender());
            System.out.println("현재 인원: " + room.getUsers().size() + "명");

            message.setContent(message.getSender() + "님이 입장하셨습니다.");
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", message);
        }
    }

    public void startNextTurn(String roomId, String winnerId) {
        GameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        if (room.getCurrentRound() >= room.getMaxRounds()) {
            sendGameOver(room);
            return;
        }

        if (winnerId != null) {
            room.setDrawer(winnerId); // 맞힌 사람에게 턴 넘기기
        } else {
            room.nextRandomDrawer(); // 첫 판이거나 예외 상황
        }

        // 단어 선택지 전송 로직
        List<String> candidates = wordsRepo.findRandomWords().stream()
                .map(WordsEntity::getWord).toList();
        if (candidates.isEmpty()) candidates = List.of("사과", "바나나");

        GameMessage selectMsg = GameMessage.builder()
                .type("SELECT_WORD")
                .sender("SYSTEM")
                .drawerId(room.getCurrentDrawerId())
                .candidates(candidates)
                .currentRound(room.getCurrentRound() + 1) // 현재 라운드 정보
                .maxRounds(room.getMaxRounds())
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", selectMsg);
    }

    private void sendGameOver(GameRoom room) {
        room.setPlaying(false);
        GameMessage overMsg = GameMessage.builder()
                .type("GAME_OVER")
                .sender("SYSTEM")
                .content("게임이 종료되었습니다!")
                .rankings(room.getRanking()) // 랭킹 리스트 담기
                .build();
        messagingTemplate.convertAndSend("/topic/" + room.getRoomId() + "/chat", overMsg);
    }

    public void Start(String roomId) {
        GameRoom room = roomService.findRoom(roomId);
        if (room != null && !room.isPlaying()) {
            room.resetGame();

            startNextTurn(roomId, null); // null이면 랜덤 선택
        }
    }

    public void chooseWord(String roomId, String chosenWord) {
        GameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        room.setCurrentAnswer(chosenWord);
        room.setPlaying(true);

        GameMessage startMsg = GameMessage.StartTypeMessageWithContent(room, "게임 시작! 출제자가 단어를 선택했습니다.");
        messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", startMsg);
    }

    public void inputWord(String roomId, String inputWord) {
        GameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        if(inputWord != null && !inputWord.trim().isEmpty()) {
            if(!wordsRepo.existsByWord(inputWord)) {
                wordsRepo.save(new WordsEntity(null, inputWord));
                System.out.println("새로운 단어 DB 저장: " + inputWord);
            }
            room.setCurrentAnswer(inputWord);
            room.setPlaying(true);

            GameMessage startMsg = GameMessage.StartTypeMessageWithContent(room, "게임 시작! 출제자가 직접 단어를 입력했습니다.");
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", startMsg);
        }
    }

    public void exit(String roomId, GameMessage message){
        GameRoom room = roomService.findRoom(roomId);

        if (room != null) {
            // 이미 나간 유저라면 로직 중단 (이벤트 중복 방지)
            if (!room.removeUser(new Player(message.getSender(), message.getSenderId()))) {
                return;
            }

            // 퇴장 알림 전송
            message.setContent(message.getSender() + "님이 퇴장하셨습니다.");
            messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", message);

            // 남은 인원이 0명이면 방 삭제 (메모리 해제)
            if(room.getUsers().isEmpty()) {
                roomService.deleteRoom(roomId);
                System.out.println("방(" + roomId + ")이 삭제되었습니다. (인원 0명)");
            }
            // 게임 중인데 혼자 남았을 경우
            else if (room.getUsers().size() < 2 && room.isPlaying()) {
                room.stop();
                room.resetGame(); // 상태 초기화
                GameMessage stopMessage = GameMessage.SystemChatMessage("플레이어가 부족하여 게임을 중단합니다.");
                messagingTemplate.convertAndSend("/topic/" + roomId + "/chat", stopMessage);
            }
        }
    }
}
