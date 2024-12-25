package com.inix.omqweb.DTO;

import com.inix.omqweb.Donation.Donation;
import com.inix.omqweb.Donation.PlayerDonationDTO;
import com.inix.omqweb.osuAPI.Player;
import lombok.Data;
import lombok.Setter;

import java.util.List;

@Data
@Setter
public class LeaderboardDTO {
    private List<Player> topPlayers;
    private List<PlayerDonationDTO> topDonators;
    private List<Donation> recentDonations;
    private List<SeasonalLeaderboardDTO> topRecentPlayers;
    private int totalItems;
}
