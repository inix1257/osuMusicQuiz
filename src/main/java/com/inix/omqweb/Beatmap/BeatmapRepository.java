package com.inix.omqweb.Beatmap;

import com.inix.omqweb.DTO.BeatmapRangeUpdateDTO;
import com.inix.omqweb.DTO.BeatmapStatsDTO;
import com.inix.omqweb.Game.GameMode;
import com.inix.omqweb.Game.GuessMode;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface BeatmapRepository extends JpaRepository<Beatmap, Integer> {
    List<Beatmap> findBeatmapsByCreator(String creator);

    List<Beatmap> findBeatmapsByTitle(String title);

    List<Beatmap> findBeatmapsByArtist(String artist);

    @Query(value = "SELECT * FROM beatmap ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Beatmap> findRandom(int limit);

    @Query(value = "SELECT DISTINCT b.title FROM beatmap b " +
            "JOIN beatmap_stats bs ON b.beatmapset_id = bs.beatmapset_id " +
            "WHERE b.beatmapset_id IN (SELECT bs_inner.beatmapset_id FROM beatmap_stats bs_inner WHERE bs_inner.game_mode = :gameMode) " +
            "AND b.deleted = false", nativeQuery = true)
    List<String> findDistinctTitles(String gameMode);

    @Query(value = "SELECT DISTINCT b.artist FROM beatmap b " +
            "JOIN beatmap_stats bs ON b.beatmapset_id = bs.beatmapset_id " +
            "WHERE b.beatmapset_id IN (SELECT bs_inner.beatmapset_id FROM beatmap_stats bs_inner WHERE bs_inner.game_mode = :gameMode) " +
            "AND b.deleted = false", nativeQuery = true)
    List<String> findDistinctArtists(String gameMode);

    @Query(value = "SELECT DISTINCT b.creator FROM beatmap b " +
            "JOIN beatmap_stats bs ON b.beatmapset_id = bs.beatmapset_id " +
            "WHERE b.beatmapset_id IN (SELECT bs_inner.beatmapset_id FROM beatmap_stats bs_inner WHERE bs_inner.game_mode = :gameMode) " +
            "AND b.deleted = false", nativeQuery = true)
    List<String> findDistinctCreators(String gameMode);

    @Query("SELECT DISTINCT b.title FROM Beatmap b")
    List<String> findDistinctTitles();

    @Query("SELECT DISTINCT b.artist FROM Beatmap b")
    List<String> findDistinctArtists();

    @Query("SELECT DISTINCT b.creator FROM Beatmap b")
    List<String> findDistinctCreators();

    @Query(value = "SELECT b.*, (SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) AS total_guess_rate FROM beatmap b " +
            "JOIN beatmap_stats bs ON b.beatmapset_id = bs.beatmapset_id " +
            "WHERE bs.guess_mode = :guessMode AND b.deleted = false " +
            "GROUP BY b.beatmapset_id, bs.guess_mode " +
            "ORDER BY (SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) DESC", nativeQuery = true)
    List<Beatmap> findBeatmapsByAnswerRate(@Param("guessMode") String guessMode);

    @Query(value = "SELECT b.*, (SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) as total_guess_rate FROM beatmap b " +
            "JOIN beatmap_stats bs ON b.beatmapset_id = bs.beatmapset_id " +
            "WHERE b.beatmapset_id IN (SELECT bs_inner.beatmapset_id FROM beatmap_stats bs_inner WHERE bs_inner.guess_mode = :guessMode AND bs_inner.game_mode = :gameMode) " +
            "AND b.approved_date BETWEEN :startDate AND :endDate " +
            "AND bs.guess_mode = :guessMode " +
            "AND b.tags LIKE :tags " +
            "AND b.genre IN :genres " +
            "AND b.language IN :languages " +
            "AND b.deleted = false " +
            "GROUP BY b.beatmapset_id, bs.guess_mode " +
            "HAVING ((SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) BETWEEN :diffL1 AND :diffU1 " +
            "OR (SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) BETWEEN :diffL2 AND :diffU2 " +
            "OR (SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) BETWEEN :diffL3 AND :diffU3 " +
            "OR (SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) BETWEEN :diffL4 AND :diffU4 " +
            "OR (SUM(bs.guessed) / SUM(IF(bs.played = 0, 1, bs.played))) BETWEEN :diffL5 AND :diffU5) " +
            "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Beatmap> findBeatmapsByApprovedDateRangeAndDifficultyAndGuessMode(
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate,
            @Param("diffL1") double diffL1,
            @Param("diffU1") double diffU1,
            @Param("diffL2") double diffL2,
            @Param("diffU2") double diffU2,
            @Param("diffL3") double diffL3,
            @Param("diffU3") double diffU3,
            @Param("diffL4") double diffL4,
            @Param("diffU4") double diffU4,
            @Param("diffL5") double diffL5,
            @Param("diffU5") double diffU5,
            @Param("limit") int limit,
            @Param("tags") String tags,
            @Param("genres") List<Integer> genres,
            @Param("languages") List<Integer> languages,
            @Param("gameMode") String gameMode,
            @Param("guessMode") String guessMode);

    @Query(value = "SELECT * FROM beatmap WHERE genre IS NULL ORDER BY RAND() LIMIT 100", nativeQuery = true)
    List<Beatmap> findRandomBeatmapsWithNoTags();

    @Query(value = "SELECT * FROM beatmap WHERE title LIKE '%(%Size)%' ORDER BY RAND() LIMIT 100", nativeQuery = true)
    List<Beatmap> findRandomBeatmapsWithMarkers();

    List<Beatmap> findAll(Specification<Beatmap> spec);

    @Query(value = "SELECT * FROM beatmap b WHERE b.beatmapset_id NOT IN (SELECT d.beatmapset_id FROM daily_guess d) ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Beatmap findRandomBeatmapNotInDailyGuess();

    @Query(value = "SELECT * FROM beatmap WHERE beatmapset_id = :beatmapset_id", nativeQuery = true)
    Beatmap findBeatmapByBeatmapset_id(int beatmapset_id);
}
