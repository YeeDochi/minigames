package com.example.demo.WordsRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<WordEntity, Long> {


    boolean existsByName(String name);
    @Query(value = "SELECT * FROM kkutu_words WHERE name LIKE :prefix% ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<WordEntity> findRandomWordStartingWith(@Param("prefix") String prefix);
    List<WordEntity> findTop5ByNameStartingWith(String prefix);
    Optional<WordEntity> findByName(String name);

    List<WordEntity> findTop10ByNameStartingWith(String startingLetter);
    @Query("SELECT w FROM WordEntity w WHERE w.name LIKE :prefix% AND LENGTH(w.name) >= 2 AND w.part = :part ORDER BY RAND() LIMIT 10")
    List<WordEntity> findValidWords(@Param("prefix") String prefix, @Param("part") String part);
}