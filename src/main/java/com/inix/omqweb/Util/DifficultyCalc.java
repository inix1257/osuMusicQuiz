package com.inix.omqweb.Util;

import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.GuessMode;

public class DifficultyCalc {
    public static double[][] DIFF_BARS = new double[GuessMode.values().length][GameDifficulty.values().length + 1];

    public static GameDifficulty getDifficulty(GuessMode guessMode, double guessRate) {
        if (guessRate <= DIFF_BARS[guessMode.getValue()][GameDifficulty.EXTRA.getValue()]) {
            return GameDifficulty.EXTRA;
        } else if (guessRate <= DIFF_BARS[guessMode.getValue()][GameDifficulty.INSANE.getValue()]) {
            return GameDifficulty.INSANE;
        } else if (guessRate <= DIFF_BARS[guessMode.getValue()][GameDifficulty.HARD.getValue()]) {
            return GameDifficulty.HARD;
        } else if (guessRate <= DIFF_BARS[guessMode.getValue()][GameDifficulty.NORMAL.getValue()]) {
            return GameDifficulty.NORMAL;
        } else {
            return GameDifficulty.EASY;
        }
    }
}
