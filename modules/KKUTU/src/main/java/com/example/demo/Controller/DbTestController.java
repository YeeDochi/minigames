package com.example.demo.Controller;
import com.example.demo.WordsRepo.WordEntity; // WordEntity 경로 확인
import com.example.demo.WordsRepo.WordRepository; // WordRepository 경로 확인
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test/db")
@RequiredArgsConstructor
public class DbTestController {

    private final WordRepository wordRepository;

    @GetMapping("/{startLetter}")
    public List<String> findWordsStartingWith(@PathVariable String startLetter) {
        System.out.println("--- [API TEST] Received request for letter: [" + startLetter + "] ---");
        List<WordEntity> results = wordRepository.findTop5ByNameStartingWith(startLetter);
        System.out.println("--- [API TEST] JPA Result Count: " + results.size() + " ---");

        return results.stream()
                .map(WordEntity::getName)
                .collect(Collectors.toList());
    }
}