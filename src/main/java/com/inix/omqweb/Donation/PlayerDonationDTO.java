package com.inix.omqweb.Donation;

import com.inix.omqweb.osuAPI.Player;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class PlayerDonationDTO {
    private Player player;
    private double totalAmount;

    public PlayerDonationDTO(Player player, double totalAmount) {
        this.player = player;
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "PlayerDonationDTO{" +
                "player=" + player.getUsername() +
                ", totalAmount=" + totalAmount +
                '}';
    }

}