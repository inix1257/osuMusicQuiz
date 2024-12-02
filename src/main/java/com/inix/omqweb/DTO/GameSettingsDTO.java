package com.inix.omqweb.DTO;

import com.inix.omqweb.Beatmap.GenreType;
import com.inix.omqweb.Beatmap.LanguageType;
import com.inix.omqweb.Game.*;

import java.util.List;

public interface GameSettingsDTO {
    String getName();
    int getTotalQuestions();
    int getGuessingTime();
    int getCooldownTime();
    boolean isAutoskip();
    List<GameDifficulty> getDifficulty();
    int getStartYear();
    int getEndYear();
    GameMode getGameMode();
    GuessMode getGuessMode();
    PoolMode getPoolMode();
    List<DisplayMode> getDisplayMode();
    List<GenreType> getGenreType();
    List<LanguageType> getLanguageType();
}