package com.example.demo.bots; // 패키지 확인

import com.example.demo.DTO.GameRoom;
import com.example.demo.Event.TurnSuccessEvent;
import com.example.demo.WordsRepo.WordEntity;
import com.example.demo.WordsRepo.WordRepository;
import com.example.demo.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map; // [!!!] Map import 추가
import java.util.Set; // Set import (기존 코드에 있었을 수 있음)
import java.util.regex.Pattern; // Pattern import (기존 코드에 있었을 수 있음)

@Service
@RequiredArgsConstructor
public class AiPlayerService {

    private final WordRepository wordRepository;
    private final GameRoomService gameRoomService;
    // VALID_WORD_PATTERN은 현 로직에서 직접 사용 안 하지만, 혹시 모르니 남겨둠
    private static final Pattern VALID_WORD_PATTERN = Pattern.compile("^[가-힣]{2,}$");

    @Async
    @EventListener
    @Transactional
    public void onTurnSuccess(TurnSuccessEvent event) {
        String nextPlayerUid = event.getNextPlayerUid();

        if (nextPlayerUid != null && nextPlayerUid.startsWith("AI_BOT_")) {
            try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            String lastWord = event.getLastword();
            String startingLetter;

            if (lastWord == null) {
                startingLetter = "가";
                System.out.println(">>> AI BOT(" + nextPlayerUid + ") searching for ANY word (defaulting to '가').");
            } else {
                startingLetter = lastWord.substring(lastWord.length() - 1);
                System.out.println(">>> AI BOT(" + nextPlayerUid + ") searching for valid words starting with: [" + startingLetter + "]");
            }

            String chosenWord = null;
            String chosenDefinition = null;
            try {
                // 두음법칙 적용
                String alternativeLetter = gameRoomService.getAlternativeStartChar(startingLetter);
                List<WordEntity> potentialWords = new ArrayList<>(wordRepository.findValidWords(startingLetter, "명사"));
                if (alternativeLetter != null) {
                    System.out.println(">>> AI BOT(" + nextPlayerUid + ") Also searching for alternative: [" + alternativeLetter + "]");
                    potentialWords.addAll(wordRepository.findValidWords(alternativeLetter, "명사"));
                }
                Collections.shuffle(potentialWords);

                System.out.println(">>> AI BOT(" + nextPlayerUid + ") JPA Potential Words (Total): " + potentialWords.size());

                if (!potentialWords.isEmpty()) {
                    for (WordEntity wordEntity : potentialWords) {
                        String potentialWord = wordEntity.getName();

                        Map<String, Object> validationResult = gameRoomService.validateWordSynchronously(
                                event.getRoomId(), potentialWord, nextPlayerUid);

                        boolean isValid = (Boolean) validationResult.getOrDefault("isValid", false);

                        if (isValid) {
                            chosenWord = potentialWord;
                            chosenDefinition = (String) validationResult.get("definition");
                            System.out.println("--- AI BOT(" + nextPlayerUid + ") validation PASSED for: [" + chosenWord + "]");
                            break;
                        } else {
                            System.out.println("--- AI BOT(" + nextPlayerUid + ") validation FAILED for: [" + potentialWord + "], trying next...");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("!!! AI BOT(" + nextPlayerUid + ") Error during DB query/validation loop: " + e.getMessage());
                e.printStackTrace();
            }

            // 제출 로직
            if (chosenWord != null) {
                System.out.println("<<< AI BOT(" + nextPlayerUid + ") finally submitting word: [" + chosenWord + "]");
                gameRoomService.handleWordSubmission(
                        event.getRoomId(),
                        chosenWord,
                        nextPlayerUid,
                        chosenDefinition // [!!!] 저장된 뜻 전달
                );
            } else {
                System.err.println("!!! AI BOT(" + nextPlayerUid + ") could NOT find any valid & usable word. Passing turn.");
                gameRoomService.passTurn(event.getRoomId(), nextPlayerUid);
            }
        }
    }
}