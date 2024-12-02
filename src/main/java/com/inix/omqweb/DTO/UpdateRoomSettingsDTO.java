package com.inix.omqweb.DTO;

import com.inix.omqweb.Beatmap.GenreType;
import com.inix.omqweb.Beatmap.LanguageType;
import com.inix.omqweb.Game.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateRoomSettingsDTO implements GameSettingsDTO{
    private UUID uuid;
    private String name;

    private String password;
    private boolean isPrivate;

    private int totalQuestions;

    private int guessingTime;
    private int cooldownTime;

    private boolean autoskip;

    private List<DisplayMode> displayMode;
    private GameMode gameMode;
    List<GameDifficulty> difficulty;

    private int startYear;
    private int endYear;

    private GuessMode guessMode;
    private PoolMode poolMode;

    private List<GenreType> genreType;
    private List<LanguageType> languageType;
}
