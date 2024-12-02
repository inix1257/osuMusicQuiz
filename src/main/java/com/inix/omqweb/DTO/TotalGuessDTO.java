package com.inix.omqweb.DTO;

import lombok.Data;

import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class TotalGuessDTO {
    private int totalGuess;
    private int totalMapPlayed;
    private double totalPoints;
    private boolean isCorrect;
    private boolean isAliasCorrect;

    CopyOnWriteArrayList<Double> speedPoints = new CopyOnWriteArrayList<>();
}
