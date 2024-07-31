package com.inix.omqweb.Beatmap;

public enum LanguageType {
    ANY(0),
    UNSPECIFIED(1),
    ENGLISH(2),
    JAPANESE(3),
    CHINESE(4),
    INSTRUMENTAL(5),
    KOREAN(6),
    FRENCH(7),
    GERMAN(8),
    SWEDISH(9),
    SPANISH(10),
    ITALIAN(11),
    RUSSIAN(12),
    POLISH(13),
    OTHER(14);

    private final int value;

    LanguageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}