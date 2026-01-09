package org.example.member.controller;

import lombok.RequiredArgsConstructor;
import org.example.member.domain.GameRecord;
import org.example.member.domain.User;
import org.example.member.dto.RecordRequestDTO;
import org.example.member.repo.GameRecordRepository;
import org.example.member.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/api")
@RequiredArgsConstructor
public class InternalApiController {

    private final UserRepository userRepository;
    private final GameRecordRepository gameRecordRepository;

    @Transactional
    @PostMapping("/records")
    public ResponseEntity<?> saveRecord(@RequestBody RecordRequestDTO req,
                                        @RequestHeader("X-SERVER-KEY") String serverKey) {

        if (!"MY_SUPER_SECRET_KEY".equals(serverKey)) {
            return ResponseEntity.status(403).body("누구세요? (잘못된 서버 키)");
        }

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("유저 없음"));
        GameRecord usersRecord = gameRecordRepository.findByUserandGameType(user,req.getGameType());
        if(usersRecord == null) {
            GameRecord record;
            if(req.isScore())
                    record = new GameRecord(user, req.getGameType(), req.getScore());
            else record = new GameRecord(user, req.getGameType(), 1);
            gameRecordRepository.save(record);
        } else{
            if(req.isScore()) usersRecord.setScore(req.getScore());
            else usersRecord.setScore(usersRecord.getScore() + 1);
        }
        return ResponseEntity.ok("기록 저장 완료");
    }

    @GetMapping("/records")
    public ResponseEntity<?> getRecords(
            @RequestHeader("X-SERVER-KEY") String serverKey,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String gameType) {

        if (!"MY_SUPER_SECRET_KEY".equals(serverKey)) {
            return ResponseEntity.status(403).body("권한 없음");
        }

        List<GameRecord> records;

        if (username != null && gameType != null) {
            User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("유저 없음"));
            records = gameRecordRepository.findAllByUserAndGameType(user, gameType);
        } else if (username != null) {
            User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("유저 없음"));
            records = gameRecordRepository.findAllByUser(user);
        } else if (gameType != null) {
            records = gameRecordRepository.findAllByGameType(gameType);
        } else {
            records = gameRecordRepository.findAll();
        }

        return ResponseEntity.ok(records);
    }

}