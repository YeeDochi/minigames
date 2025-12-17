package org.example.catchmind.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordsRepo extends JpaRepository<WordsEntity,Long> {
    @Query(value = "SELECT * FROM catchmind_word ORDER BY RAND() LIMIT 3", nativeQuery = true)
    List<WordsEntity> findRandomWords();

    boolean existsByWord(String word);
}
