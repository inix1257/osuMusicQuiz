package com.inix.omqweb.History;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LobbyHistoryRepository extends JpaRepository<LobbyHistory, UUID> {
}
