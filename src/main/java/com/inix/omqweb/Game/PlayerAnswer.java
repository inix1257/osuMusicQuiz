package com.inix.omqweb.Game;

import lombok.*;

@Getter
@Setter
@Builder
@Data
@AllArgsConstructor
public class PlayerAnswer {
    private String answer;
    private Long submittedTime;
    private Long timeTaken;
    private boolean isCorrect;
    private double speedBonus;
    private double difficultyBonus;
    private double poolSizeBonus;
    private double totalPoints;
}