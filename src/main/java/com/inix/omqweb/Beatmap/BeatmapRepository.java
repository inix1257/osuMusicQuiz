package com.inix.omqweb.Beatmap;

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

    @Query(value = "SELECT * FROM beatmap ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Beatmap> findRandom(int limit);

    @Query(value = "SELECT * FROM beatmap WHERE (playcount_answer/playcount) >= 0.7 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Beatmap> findRandomEasy(int limit);

    @Query(value = "SELECT * FROM beatmap WHERE (playcount_answer/playcount) > 0.3 AND (playcount_answer/playcount) < 0.7 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Beatmap> findRandomNormal(int limit);

    @Query(value = "SELECT * FROM beatmap WHERE (playcount_answer/playcount) <= 0.3 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Beatmap> findRandomHard(int limit);

    @Query("SELECT DISTINCT b.title FROM Beatmap b")
    List<String> findDistinctTitles();

    @Query("SELECT DISTINCT b.artist FROM Beatmap b")
    List<String> findDistinctArtists();

    @Query("SELECT DISTINCT b.creator FROM Beatmap b")
    List<String> findDistinctCreators();

    @Query(value = "SELECT * FROM beatmap ORDER BY answer_rate DESC", nativeQuery = true)
    List<Beatmap> findBeatmapsByAnswerRate();

    @Query("SELECT b FROM Beatmap b WHERE b.approved_date BETWEEN :startDate AND :endDate")
    List<Beatmap> findBeatmapsByApprovedDateRange(Timestamp startDate, Timestamp endDate);

    @Query(value = "SELECT * FROM beatmap HAVING (approved_date BETWEEN :startDate AND :endDate) AND (" +
            "(answer_rate BETWEEN :diffL0 AND :diffU0) OR" +
            "(answer_rate BETWEEN :diffL1 AND :diffU1) OR" +
            "(answer_rate BETWEEN :diffL2 AND :diffU2) OR" +
            "(answer_rate BETWEEN :diffL3 AND :diffU3)) AND tags LIKE :tags AND genre IN :genres AND language IN :languages " +
            "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Beatmap> findBeatmapsByApprovedDateRangeAndDifficulty(Timestamp startDate, Timestamp endDate,
                                                               double diffL0, double diffU0, double diffL1, double diffU1, double diffL2, double diffU2, double diffL3, double diffU3,
                                                               int limit, String tags,
                                                               @Param("genres") List<Integer> genres, @Param("languages") List<Integer> languages);

    @Query(value = "SELECT COUNT(*) FROM beatmap WHERE (approved_date BETWEEN :startDate AND :endDate) AND (" +
            "((playcount_answer/playcount) BETWEEN :diffL0 AND :diffU0) OR " +
            "((playcount_answer/playcount) BETWEEN :diffL1 AND :diffU1) OR " +
            "((playcount_answer/playcount) BETWEEN :diffL2 AND :diffU2) OR " +
            "((playcount_answer/playcount) BETWEEN :diffL3 AND :diffU3)) AND tags LIKE :tags AND genre IN :genres AND language IN :languages", nativeQuery = true)
    int countBeatmapsByApprovedDateRangeAndDifficulty(Timestamp startDate, Timestamp endDate,
                                                      double diffL0, double diffU0, double diffL1, double diffU1, double diffL2, double diffU2, double diffL3, double diffU3,
                                                      String tags,
                                                      List<Integer> genres, List<Integer> languages);

    @Query(value = "SELECT * FROM beatmap WHERE genre IS NULL ORDER BY RAND() LIMIT 100", nativeQuery = true)
    List<Beatmap> findRandomBeatmapsWithNoTags();

    @Query(value = "SELECT * FROM beatmap WHERE title LIKE '%(%Size)%' ORDER BY RAND() LIMIT 100", nativeQuery = true)
    List<Beatmap> findRandomBeatmapsWithMarkers();

    List<Beatmap> findAll(Specification<Beatmap> spec);

    @Query(value = "SELECT * FROM beatmap b WHERE b.beatmapset_id NOT IN (SELECT d.beatmapset_id FROM daily_guess d) AND (answer_rate > 0.3) AND (playcount >= 100) ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Beatmap findRandomBeatmapNotInDailyGuess();
}
