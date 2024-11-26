package com.inix.omqweb.Daily;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyGuessDTO {
    private int dailyNumber;
    private int retryCount;
    private boolean guessed;
    private String base64;
    private DailyGuessLog dailyGuessLog;
}
