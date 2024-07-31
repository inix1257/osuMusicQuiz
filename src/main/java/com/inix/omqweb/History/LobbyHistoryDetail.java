package com.inix.omqweb.History;

import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.osuAPI.Player;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "history_detail")
public class LobbyHistoryDetail {
    @EmbeddedId
    private LobbyHistoryDetailId id;

    @ManyToOne
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private LobbyHistory lobbyHistory;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "beatmapset_id", insertable = false, updatable = false)
    private Beatmap beatmap;

    private String gametype;
    private boolean answer;
    private double timetaken;
    private double difficulty_bonus;
    private double speed_bonus;
    private String difficulty;
}
