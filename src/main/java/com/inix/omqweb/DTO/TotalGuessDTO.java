package com.inix.omqweb.DTO;

import lombok.Data;

import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class TotalGuessDTO {
    private int totalGuess;
    private double totalPoints;

    CopyOnWriteArrayList<Double> speedPoints = new CopyOnWriteArrayList<>();
}
