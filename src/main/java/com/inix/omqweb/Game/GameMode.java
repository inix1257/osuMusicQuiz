package com.inix.omqweb.Game;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
public enum GameMode {
    STD(0),
    TAIKO(1),
    CTB(2),
    MANIA(3),
    ANY(4);

    private final int value;

    GameMode(int value) {
        this.value = value;
    }

    public static GameMode fromValue(int value) {
        for (GameMode mode : GameMode.values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("No enum constant for value: " + value);
    }
}

