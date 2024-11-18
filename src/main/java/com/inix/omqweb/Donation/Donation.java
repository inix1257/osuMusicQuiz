package com.inix.omqweb.Donation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inix.omqweb.osuAPI.Player;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

@Entity
@Getter
@ToString
@Table(name = "donation")
public class Donation {
    @Id
    @JsonIgnore
    private int id;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    private Player player;

    private Date time;
    private double amount;

    @JsonIgnore
    private String comment;
}
