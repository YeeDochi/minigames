package org.example.catchmind.controller;

import lombok.RequiredArgsConstructor;
import org.example.catchmind.repo.WordsEntity;
import org.example.catchmind.repo.WordsRepo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WordController {
    private final WordsRepo wordsRepo;

    @GetMapping("/words/add")
    public String addWordPage() {
        return "add-word";
    }

    @PostMapping("/api/words")
    @ResponseBody
    public String addWord(@RequestParam String text) {
        if(text == null || text.trim().isEmpty()) return "실패: 단어를 입력하세요";
        try {
            wordsRepo.save(new WordsEntity(null, text.trim()));
            return "성공: " + text;
        } catch (Exception e) {
            return "실패: 이미 존재하는 단어이거나 오류 발생";
        }
    }

    @GetMapping("/api/words")
    @ResponseBody
    public List<WordsEntity> getAllWords() {
        return wordsRepo.findAll();
    }
}
