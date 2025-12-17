package com.example.demo.service; // 패키지 확인

import com.example.demo.DTO.GameRoom;
import com.example.demo.Event.TurnSuccessEvent;
import com.example.demo.service.KoreanApiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final Map<String, GameRoom> activeGameRooms = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final KoreanApiService koreanApiService;
    private static final int MAX_FAILURES = 3;
    private static final Pattern VALID_WORD_PATTERN = Pattern.compile("^[가-힣]{2,}$");


    private List<String> finishingWords = new ArrayList<>(List.of("늄", "륨", "뮴", "쁨", "슭", "걀", "녁"));

    @PostConstruct
    public void init() {
        System.out.println("Finishing words loaded: " + finishingWords);
    }

    // --- 방 생성 메소드 (변경 없음) ---
    public GameRoom createRoom(String roomName, int maxPlayers, int botCount) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        GameRoom newRoom = new GameRoom(roomId, roomName, maxPlayers, botCount);
        activeGameRooms.put(roomId, newRoom);
        System.out.println("--- [ROOM CREATED] ID: " + roomId + ", Name: " + roomName + " ---");
        return newRoom;
    }

    // --- [!!!] `addPlayerToRoom` 시그니처 및 로직 변경 (String 반환) ---
    /**
     * @return String "SUCCESS" 또는 에러 코드 (e.g., "NICKNAME_DUPLICATE")
     */
    public String addPlayerToRoom(String roomId, String uid, String nickname) {
        GameRoom room = activeGameRooms.get(roomId);
        if (room == null) {
            return "ROOM_NOT_FOUND"; // [!!!] String 반환
        }

        // --- [!!!] 중복 검사 로직 (Controller가 에러를 보낼 수 있도록) ---
        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            return "ROOM_FULL"; // [!!!] String 반환
        }
        // (서버 UID 발급 방식이므로 UID 중복은 사실상 발생 안 하지만, 방어 코드로 둠)
        if (room.getPlayers().stream().anyMatch(p -> p.getUid().equals(uid))) {
            return "UID_DUPLICATE"; // [!!!] String 반환
        }
        // --- [!!!] 검사 끝 ---

        // GameRoom.addPlayer는 이제 검사를 통과했으므로 true를 반환해야 함
        // (GameRoom.java의 중복 검사는 이제 중복임)
        boolean success = room.addPlayer(uid, nickname);

        if (success) {
            // 성공 방송 (변경 없음)
            messagingTemplate.convertAndSend("/topic/game-room/" + roomId,
                    "새로운 유저 입장: " + nickname + " (현재 인원: " + room.getPlayers().size() + "/" + room.getMaxPlayers() + ")");

            if (room.getPlayers().size() - room.getBotCount() == 1) {
                String firstPlayerNickname = room.getCurrentPlayer().getNickname();
                messagingTemplate.convertAndSend("/topic/game-room/" + roomId,
                        "게임 시작! 첫 턴은 " + firstPlayerNickname + "님입니다.");
            }
            return "SUCCESS"; // [!!!] String 반환
        } else {
            // (이론상 여기에 도달하면 안 됨, GameRoom.java의 검사와 중복되기 때문)
            return "ADD_PLAYER_FAILED"; // Fallback
        }
    }


    // --- `validateWordSynchronously` 시그니처 변경 (uid) ---
    public Map<String, Object> validateWordSynchronously(String roomId, String word, String uid) {
        GameRoom room = activeGameRooms.get(roomId);

        // [!!!] HashMap 사용으로 변경 (null 허용) ---
        Map<String, Object> failResult = new HashMap<>();
        failResult.put("isValid", false);
        failResult.put("definition", null);
        // --- [!!!] ---

        // 기본 검사 실패 시
        if (room == null || word == null || word.isEmpty()) {
            System.out.println("--- [SYNC VALIDATE FAIL] Word: [" + word + "] - Room not found or word is empty");
            return failResult; // [!!!] HashMap 반환
        }
        if (!VALID_WORD_PATTERN.matcher(word).matches()) {
            System.out.println("--- [SYNC VALIDATE FAIL] Word: [" + word + "] - Invalid pattern");
            return failResult; // [!!!] HashMap 반환
        }
        // 한방 단어 방지
        if (finishingWords != null && finishingWords.stream().anyMatch(word::endsWith)) {
            System.out.println("--- [SYNC VALIDATE FAIL] Word: [" + word + "] - Ends with a forbidden character. Forbidden list: " + finishingWords);
            return failResult;
        }
        if (room.getLastWord() != null) {
            String lastCharStr = room.getLastWord().substring(room.getLastWord().length() - 1);
            String alternativeStartStr = getAlternativeStartChar(lastCharStr);
            boolean rulePass = word.startsWith(lastCharStr) || (alternativeStartStr != null && word.startsWith(alternativeStartStr));
            if (!rulePass) {
                System.out.println("--- [SYNC VALIDATE FAIL] Word: [" + word + "] - Rule mismatch (Last: " + lastCharStr + ")");
                return failResult; // [!!!] HashMap 반환
            }
        }
        if (room.getUsedWords().contains(word)) {
            System.out.println("--- [SYNC VALIDATE FAIL] Word: [" + word + "] - Already used");
            return failResult; // [!!!] HashMap 반환
        }

        // API 호출 (이제 Map 반환)
        Map<String, Object> apiResult = koreanApiService.validateWord(word);

        if (!(Boolean) apiResult.getOrDefault("isValid", false)) {
            System.out.println("--- [SYNC VALIDATE FAIL] Word: [" + word + "] - Failed API validation");
            // apiResult는 이미 isValid=false, definition=null 또는 실제값 을 포함
        } else {
            System.out.println("--- [SYNC VALIDATE SUCCESS] Word: [" + word + "] (User: " + uid + ")");
            // apiResult는 isValid=true, definition=실제값 을 포함
        }
        return apiResult; // apiResult는 null 값을 포함할 수 있는 HashMap임
    }

    // --- `handleWordSubmission` 시그니처 변경 (uid) ---
    public void handleWordSubmission(String roomId, String word, String uid, String definition) {
        GameRoom room = activeGameRooms.get(roomId);
        if (room == null) return;

        // 턴 체크
        if (room.getCurrentPlayer() == null || !room.getCurrentPlayer().getUid().equals(uid)) {
            System.err.println("!!! ERROR in handleWordSubmission - Not player's turn.");
            return;
        }
        // 중복 단어 체크 (봇이 실수할 경우 대비)
        if (room.getUsedWords().contains(word)) {
            System.err.println("!!! ERROR in handleWordSubmission - Word already used by BOT?: " + word);
            // 봇이 중복 제출하면 그냥 턴을 넘김
            passTurn(roomId, uid);
            return;
        }

        // [!!!] processValidationResult 호출 시 전달받은 definition 전달
        // (봇이 제출하는 단어는 AiPlayerService에서 이미 검증했으므로 isValid=true)
        processValidationResult(roomId, uid, word, true, definition);
    }

    // --- `handleSubmitFromPlayer` 시그니처 변경 (uid) ---
    public void handleSubmitFromPlayer(String roomId, String word, String uid) {
        GameRoom room = activeGameRooms.get(roomId);
        if (room == null || room.getCurrentPlayer() == null || !room.getCurrentPlayer().getUid().equals(uid)) {
            System.err.println("!!! ERROR in handleSubmitFromPlayer - Room or Turn mismatch.");
            return;
        }

        // [!!!] 동기 검증 결과 (Map) 받기
        Map<String, Object> validationResult = validateWordSynchronously(roomId, word, uid);
        boolean isValid = (Boolean) validationResult.getOrDefault("isValid", false);
        String definition = (String) validationResult.get("definition"); // 실패 시 null

        // [!!!] processValidationResult 호출 시 definition 전달
        processValidationResult(roomId, uid, word, isValid, definition);
    }

    // --- `processValidationResult` 시그니처 변경 (uid) ---
    public void processValidationResult(String roomId, String uid, String word, boolean isValid, String definition) {
        GameRoom room = activeGameRooms.get(roomId);
        if (room == null) return;

        String topic = "/topic/game-room/" + roomId;
        String nickname = room.getNicknameByUid(uid);

        if (isValid) {
            // [!!!] 메시지 포맷 변경 ---
            String previousWord = room.getLastWord(); // 이전 단어 가져오기

            // 상태 업데이트 (순서 중요: previousWord 먼저 가져오고 lastWord 업데이트)
            room.resetFailureCount(uid);
            room.getUsedWords().add(word);
            room.setLastWord(word); // 현재 단어를 다음을 위해 저장

            // 새 메시지 생성
            String successMessage = (previousWord != null ? previousWord + " -> " : "") +
                    word + " (성공! 뜻: " + (definition != null ? definition : "정보 없음") + ")";

            // [!!!] 변경된 메시지 전송
            messagingTemplate.convertAndSend(topic, successMessage);

            // 다음 턴 진행 로직 (기존과 동일)
            GameRoom.PlayerInfo nextPlayer = room.getNextPlayer();
            if (nextPlayer == null) {
                System.err.println("!!! ERROR in processValidationResult - Next player is null.");
                return;
            }
            // [!!!] 다음 턴 알림 메시지 (별도 전송)
            Map<String, String> turnSignal = new HashMap<>();
            turnSignal.put("type", "TURN_CHANGE");
            turnSignal.put("nextPlayer", nextPlayer.getNickname());
            messagingTemplate.convertAndSend(topic, turnSignal);

            eventPublisher.publishEvent(new TurnSuccessEvent(this, roomId, nextPlayer.getUid(), word));
            // --- [!!!] 메시지 포맷 변경 끝 ---

        } else {
            // 실패 로직 (기존과 동일)
            System.out.println("--- [PROCESS RESULT FAIL] Room: " + roomId + ", User: " + nickname + ", Word: [" + word + "] ---");
            if (!uid.startsWith("AI_BOT_")) {
                int failures = room.incrementFailureCount(uid);
                messagingTemplate.convertAndSend(topic,
                        "'" + word + "' (은)는 유효하지 않은 단어입니다. " + nickname + "님 다시 시도하세요. (실패: " + failures + "/" + MAX_FAILURES + ")");
                if (failures >= MAX_FAILURES) {
                    eliminatePlayer(roomId, uid, "실패 3회 초과");
                }
            } else {
                System.err.println("!!! BOT validation failed unexpectedly: " + word);
                passTurn(roomId, uid);
            }
        }
    }

    // --- `passTurn` 시그니처 변경 (uid) ---
    public void passTurn(String roomId, String uid) {
        GameRoom room = activeGameRooms.get(roomId);
        if (room == null || room.getCurrentPlayer() == null || !room.getCurrentPlayer().getUid().equals(uid)) {
            System.err.println("!!! ERROR in passTurn - Room not found or not player's turn.");
            return;
        }

        String nickname = room.getNicknameByUid(uid); // 닉네임 조회
        System.out.println("--- [PASS TURN & ELIMINATE] User: " + nickname + " in room " + roomId + " ---");

        messagingTemplate.convertAndSend(
                "/topic/game-room/" + roomId,
                nickname + "님이 턴을 포기했습니다." // 닉네임
        );

        eliminatePlayer(roomId, uid, "턴 포기"); // uid
    }

    // --- `eliminatePlayer` 시그니처 변경 (uid) ---
    private void eliminatePlayer(String roomId, String uid, String reason) {
        GameRoom room = activeGameRooms.get(roomId);
        if (room == null || room.getPlayerByUid(uid) == null) {
            return;
        }

        String nickname = room.getNicknameByUid(uid); // 닉네임 조회
        System.out.println("--- [ELIMINATE PLAYER] Room: " + roomId + ", User: " + nickname + ", Reason: " + reason + " ---");

        GameRoom.PlayerInfo currentPlayer = room.getCurrentPlayer();
        String eliminatedPlayerCurrentTurnUid = (currentPlayer != null) ? currentPlayer.getUid() : null; // [!!!] uid (NPE 방지)

        room.removePlayer(uid); // uid

        messagingTemplate.convertAndSend(
                "/topic/game-room/" + roomId,
                nickname + "님이 탈락했습니다. (" + reason + ") 남은 인원: " + room.getPlayers().size() + "명" // 닉네임
        );

        checkRoomStatusAndProceed(roomId, room, uid, eliminatedPlayerCurrentTurnUid); // uid
    }


    public Map<String, GameRoom> getActiveGameRooms() {
        return activeGameRooms;
    }

    // --- `handlePlayerDisconnect` 시그니처 변경 (uid) ---
    public void handlePlayerDisconnect(String roomId, String uid) {
        GameRoom room = activeGameRooms.get(roomId);
        if (room == null || room.getPlayerByUid(uid) == null) {
            System.out.println("--- [DISCONNECT] Room or Player not found. Room: " + roomId + ", User: " + uid);
            return;
        }

        String nickname = room.getNicknameByUid(uid); // 닉네임 조회
        System.out.println("--- [DISCONNECT] Player disconnected: " + nickname + " from room " + roomId);

        GameRoom.PlayerInfo currentPlayer = room.getCurrentPlayer();
        String disconnectedPlayerCurrentTurnUid = (currentPlayer != null) ? currentPlayer.getUid() : null; // [!!!] uid (NPE 방지)

        room.removePlayer(uid); // uid

        messagingTemplate.convertAndSend(
                "/topic/game-room/" + roomId,
                nickname + "님이 퇴장했습니다. 남은 인원: " + room.getPlayers().size() + "명" // 닉네임
        );

        checkRoomStatusAndProceed(roomId, room, uid, disconnectedPlayerCurrentTurnUid); // uid
    }

    // --- `checkRoomStatusAndProceed` 시그니처 변경 (uid) ---
    private void checkRoomStatusAndProceed(String roomId, GameRoom room, String removedUid, String turnBeforeRemovalUid) {
        List<GameRoom.PlayerInfo> remainingPlayers = room.getPlayers(); // PlayerInfo
        String topic = "/topic/game-room/" + roomId;

        if (remainingPlayers.isEmpty()) {
            System.out.println("--- [ROOM REMOVE] Room is empty, removing: " + roomId);
            activeGameRooms.remove(roomId);

        } else if (remainingPlayers.stream().allMatch(GameRoom.PlayerInfo::isBot)) { // isBot 헬퍼 사용
            System.out.println("--- [ROOM REMOVE] Only bots left, removing: " + roomId);
            messagingTemplate.convertAndSend(topic, "모든 플레이어가 나가서 게임이 종료됩니다.");
            activeGameRooms.remove(roomId);

        } else if (remainingPlayers.size() == 1) {
            System.out.println("--- [GAME END] Only one player left in room: " + roomId);
            String winnerNickname = remainingPlayers.get(0).getNickname();
            String jsonMessage = String.format("{\"type\":\"GAME_OVER\", \"winner\":\"%s\"}", winnerNickname);

            messagingTemplate.convertAndSend(topic, jsonMessage);

        } else {
            room.setLastWord(null);
            System.out.println("--- [LAST WORD RESET] Last word set to null due to player elimination.");

            GameRoom.PlayerInfo nextPlayer; // PlayerInfo

            // [!!!] NPE 방지
            if (removedUid != null && removedUid.equals(turnBeforeRemovalUid)) { // uid
                nextPlayer = room.getNextPlayer();
            } else {
                nextPlayer = room.getCurrentPlayer();
            }

            if (nextPlayer == null) {
                System.err.println("!!! ERROR in checkRoomStatusAndProceed - Next player is null.");
                // 비정상 상태 복구 시도
                nextPlayer = room.getNextPlayer(); // 0번 인덱스로 강제 순환
                if(nextPlayer == null) {
                    System.err.println("!!! CRITICAL ERROR - Cannot find next player.");
                    return;
                }
            }

            System.out.println("--- [TURN PROCEED] Room: " + roomId + ", Next Player: " + nextPlayer.getNickname() + " ---");

            // `eliminatePlayer`에서 탈락 메시지를 이미 보냈으므로 여기서는 턴 시작만 알림
            messagingTemplate.convertAndSend(topic,
                    nextPlayer.getNickname() + "님부터 (아무 단어나) 다시 시작하세요."
            );

            // 다음 턴 이벤트 (uid)
            eventPublisher.publishEvent(new TurnSuccessEvent(
                    this,
                    roomId,
                    nextPlayer.getUid(),
                    null
            ));
        }
    }

    // --- 두음법칙 헬퍼 (수정 불필요) ---
    private static final char[] CHOSEONG_LIST = { 'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ' };
    private static final char[] JUNGSEONG_LIST = { 'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ' };
    private static final char[] JONGSEONG_LIST = { '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ' };
    private static final int HANGUL_START = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    public boolean isHangul(char c) { return c >= HANGUL_START && c <= HANGUL_END; }
    public String getAlternativeStartChar(String lastCharStr) {
        if (lastCharStr == null || lastCharStr.length() != 1) return null;
        char lastChar = lastCharStr.charAt(0);
        if (!isHangul(lastChar)) return null;
        int base = lastChar - HANGUL_START;
        int choseongIndex = base / (21 * 28);
        int jungseongIndex = (base % (21 * 28)) / 28;
        int jongseongIndex = base % 28;
        char choseong = CHOSEONG_LIST[choseongIndex];
        if (choseong != 'ㄹ' && choseong != 'ㄴ') return null;
        char jungseong = JUNGSEONG_LIST[jungseongIndex];
        int newChoseongIndex = -1;
        if (choseong == 'ㄹ') {
            if ("ㅣㅑㅕㅖㅛㅠㅟ".indexOf(jungseong) >= 0) newChoseongIndex = 11; // 'ㅇ'
            else newChoseongIndex = 2; // 'ㄴ'
        } else if (choseong == 'ㄴ') {
            if ("ㅣㅑㅕㅖㅛㅠ".indexOf(jungseong) >= 0) newChoseongIndex = 11; // 'ㅇ'
        }
        if (newChoseongIndex != -1) {
            int newCharBase = (newChoseongIndex * 21 * 28) + (jungseongIndex * 28) + jongseongIndex;
            char newChar = (char) (HANGUL_START + newCharBase);
            return String.valueOf(newChar);
        }
        return null;
    }
}