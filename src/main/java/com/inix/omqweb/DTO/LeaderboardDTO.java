package com.inix.omqweb.DTO;

import com.inix.omqweb.osuAPI.Player;
import lombok.Data;
import lombok.Setter;

import java.util.List;

@Data
@Setter
public class LeaderboardDTO {
    private List<Player> players;
    private int totalItems;
}
