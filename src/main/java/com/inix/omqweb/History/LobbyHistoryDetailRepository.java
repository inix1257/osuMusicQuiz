package com.inix.omqweb.History;

import com.inix.omqweb.osuAPI.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LobbyHistoryDetailRepository extends JpaRepository<LobbyHistoryDetail, LobbyHistoryDetailId> {
    List<LobbyHistoryDetail> findLobbyHistoryDetailByPlayer(Player player);
}
