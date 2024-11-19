package com.inix.omqweb.History;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface LobbyHistoryRepository extends JpaRepository<LobbyHistory, UUID> {
    @Query("SELECT lh FROM LobbyHistory lh WHERE lh.time < :time ORDER BY lh.time ASC LIMIT 10")
    List<LobbyHistory> findTop10OldestHistories(LocalDateTime time);
}
