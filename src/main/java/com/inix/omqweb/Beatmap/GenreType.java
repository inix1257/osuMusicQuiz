package com.inix.omqweb.Beatmap;

public enum GenreType {
    ANY(0),
    UNSPECIFIED(1),
    VIDEO_GAME(2),
    ANIME(3),
    ROCK(4),
    POP(5),
    OTHER(6),
    NOVELTY(7),
    UNUSED(8),
    HIP_HOP(9),
    ELECTRONIC(10),
    METAL(11),
    CLASSICAL(12),
    FOLK(13),
    JAZZ(14);

    private final int value;

    GenreType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}