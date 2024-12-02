package com.inix.omqweb.Game;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
public enum GameDifficulty {
    EASY(0),
    NORMAL(1),
    HARD(2),
    INSANE(3),
    EXTRA(4);

    private final int value;

    GameDifficulty(int value) {
        this.value = value;
    }
}
