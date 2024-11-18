package com.inix.omqweb.Daily;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyGuessDTO {
    private int dailyNumber;
    private String base64;
}
