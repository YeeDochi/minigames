package org.example.indian_poker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class Card {
    private String suit; // S, D, H, C
    private int rank;    // 1(A) ~ 13(K)

    @Override
    public String toString() {
        return suit + rank;
    }
}