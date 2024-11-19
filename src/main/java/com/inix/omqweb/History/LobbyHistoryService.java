package com.inix.omqweb.History;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LobbyHistoryService {
    private final LobbyHistoryDetailRepository lobbyHistoryDetailRepository;
    private final LobbyHistoryRepository lobbyHistoryRepository;

    @PostConstruct
    @Scheduled(fixedDelay = 1000 * 60 * 30) // Cleanup every 30 minutes
    private void deleteOldHistory() {
        // Delete old history older than 1 week
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<LobbyHistory> lobbyHistories = lobbyHistoryRepository.findTop10OldestHistories(oneWeekAgo);

        for (LobbyHistory lobbyHistory : lobbyHistories) {
            List<LobbyHistoryDetail> lobbyHistoryDetails = lobbyHistoryDetailRepository.findLobbyHistoryDetailByLobbyHistory_Lobby_id(lobbyHistory.getLobby_id());
            lobbyHistoryDetailRepository.deleteAll(lobbyHistoryDetails);
        }

        lobbyHistoryRepository.deleteAll(lobbyHistories);
    }
}
