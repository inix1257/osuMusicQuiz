package com.inix.omqweb.DTO;

import com.inix.omqweb.Beatmap.GenreType;
import com.inix.omqweb.Beatmap.LanguageType;
import com.inix.omqweb.Game.DisplayMode;
import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.GameMode;
import com.inix.omqweb.Game.PoolMode;

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
    PoolMode getPoolMode();
    List<DisplayMode> getDisplayMode();
    List<GenreType> getGenreType();
    List<LanguageType> getLanguageType();
}