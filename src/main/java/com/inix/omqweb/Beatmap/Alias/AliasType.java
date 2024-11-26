package com.inix.omqweb.Beatmap.Alias;

import lombok.Getter;

@Getter
public enum AliasType {
    ARTIST(0),
    TITLE(1),
    CREATOR(2);

    private final int value;

    AliasType(int value) {
        this.value = value;
    }

}
