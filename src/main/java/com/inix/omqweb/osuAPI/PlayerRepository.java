package com.inix.omqweb.osuAPI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {
    List<Player> findByBanFalseOrderByPointsDesc(Pageable pageable);

    int countByPointsGreaterThanAndBanFalse(BigDecimal points);

    @Query(value = "SELECT * FROM Player ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Player> findRandomPlayers();
}
