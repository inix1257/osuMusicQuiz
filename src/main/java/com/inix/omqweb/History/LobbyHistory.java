package com.inix.omqweb.History;

import com.inix.omqweb.osuAPI.Player;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "history_lobby")
public class LobbyHistory {
    @Id
    private String session_id;
    private String lobby_id;
    private Date time;
    private String difficulties;
    private int total_questions;
    private String lobby_title;
    private int guess_time;
    private int cooldown_time;
    private int start_year;
    private int end_year;
    private double poolsize_bonus;
    private String pool_mode;
    private String genre;
    private String language;

    @ManyToOne
    @JoinColumn(name = "host_id", referencedColumnName = "id")
    private Player host;
}