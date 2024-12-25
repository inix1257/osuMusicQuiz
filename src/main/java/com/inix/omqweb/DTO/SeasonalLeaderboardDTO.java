package com.inix.omqweb.DTO;

import com.inix.omqweb.osuAPI.Player;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class SeasonalLeaderboardDTO {
    private String id;
    private String username;
    private String avatar_url;
    private double points;
}
