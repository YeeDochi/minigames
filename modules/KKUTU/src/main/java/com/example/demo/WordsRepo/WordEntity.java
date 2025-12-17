package com.example.demo.WordsRepo;

import jakarta.persistence.*; // [주의] javax.persistence가 아닐 수 있습니다.
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "kkutu_words")
public class WordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", columnDefinition = "TEXT") // SQL 파일 정의에 맞춰 TEXT 타입 명시 (VARCHAR 대신)
    private String name; // 단어

    // 품사 정보를 저장할 필드 추가
    @Column(name = "part", columnDefinition = "TEXT")
    private String part; // 품사 (예: "명사", "동사")

}