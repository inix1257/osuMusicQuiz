package com.inix.omqweb.DTO;


import com.inix.omqweb.Beatmap.GenreType;
import com.inix.omqweb.Beatmap.LanguageType;
import com.inix.omqweb.Game.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
public class CreateGameDTO implements GameSettingsDTO{
    private String name;

    private String password;

    private int totalQuestions;

    private int guessingTime;
    private int cooldownTime;

    private boolean autoskip;

    private List<GameDifficulty> difficulty;
    private GameMode gameMode;
    private GuessMode guessMode;

    private int startYear;
    private int endYear;

    private PoolMode poolMode;
    private List<DisplayMode> displayMode;

    private List<GenreType> genreType;
    private List<LanguageType> languageType;

    private boolean forceLeave;
}
