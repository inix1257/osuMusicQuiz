package com.inix.omqweb.Game;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
public enum GuessMode {
    TITLE(0),
    ARTIST(1),
    CREATOR(2),
    PATTERN(3);

    private final int value;

    GuessMode(int value) {
        this.value = value;
    }
}
