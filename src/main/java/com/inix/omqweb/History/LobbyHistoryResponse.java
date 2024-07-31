package com.inix.omqweb.History;

import com.inix.omqweb.Beatmap.Beatmap;
import lombok.Data;

@Data
public class LobbyHistoryResponse {
    private String lobby_title;
    private String lobby_id;
    private String session_id;
    private Beatmap beatmap;
}
