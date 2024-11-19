package com.inix.omqweb.History;

import com.inix.omqweb.osuAPI.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LobbyHistoryDetailRepository extends JpaRepository<LobbyHistoryDetail, LobbyHistoryDetailId> {
    List<LobbyHistoryDetail> findLobbyHistoryDetailByPlayer(Player player);

    @Query("SELECT lhd FROM LobbyHistoryDetail lhd WHERE lhd.lobbyHistory.lobby_id = :lobby_id")
    List<LobbyHistoryDetail> findLobbyHistoryDetailByLobbyHistory_Lobby_id(String lobby_id);
}
