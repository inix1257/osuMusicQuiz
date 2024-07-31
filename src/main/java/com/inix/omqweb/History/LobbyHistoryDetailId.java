package com.inix.omqweb.History;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LobbyHistoryDetailId implements Serializable {
    private String session_id;
    private String player_id;
    private int beatmapset_id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LobbyHistoryDetailId)) return false;
        LobbyHistoryDetailId that = (LobbyHistoryDetailId) o;
        return session_id.equals(that.session_id) && player_id.equals(that.player_id) && beatmapset_id == that.beatmapset_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(session_id, player_id, beatmapset_id);
    }
}
