package com.inix.omqweb.Util;

public class AnswerUtil {
    public static double checkAnswer(String msg, String answer){
        msg = msg.toLowerCase();
        answer = answer.toLowerCase();

        return getSimilarity(msg, answer);
    }

    static private double getSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;

        if (s1.length() < s2.length()) {
            longer = s2;
            shorter = s1;
        }

        int longerLength = longer.length();
        if (longerLength == 0) return 1.0;
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    static private int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        int[] costs = new int[s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];

                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }

                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }

            if (i > 0) costs[s2.length()] = lastValue;
        }

        return costs[s2.length()];
    }

    public static double getSpeedBonus(Long timeDiffMillis) {
        double timeDiffInSec = timeDiffMillis / 1000d;
        double bonus = 0.1d * Math.log(1d / timeDiffInSec) + 1.3;

        return Math.min(Math.max(bonus, 1d), 1.3d); // 1 ~ 1.3d
    }

    public static double getDifficultyBonus(double guessRate) {
        // guessRate = 0.0d ~ 1.0d
        double bonus = 2 * Math.log(1d / guessRate) + 1;

        return Math.min(Math.max(bonus, 1d), 5d); // 1 ~ 5
    }

    public static double getPoolSizeBonus(int poolSize) {
        // poolSize = 0 ~ 100
        double bonus = 0.1 * Math.pow(poolSize, 0.4); // 0.0 ~ 316 (approx)

        return Math.min(Math.max(bonus, 0.3d), 1d); // 0.3 ~ 1
    }
}
