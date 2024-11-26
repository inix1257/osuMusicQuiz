package com.inix.omqweb.BeatmapReport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeatmapReportRepository extends JpaRepository<BeatmapReport, Integer> {
    @Query(value = "SELECT * FROM report_beatmap WHERE beatmapset_id = :beatmapsetId", nativeQuery = true)
    List<BeatmapReport> findAllByBeatmapsetId(int beatmapsetId);

    @Query(value = """
            SELECT rb.beatmapset_id AS beatmapsetId, COUNT(DISTINCT rb.user_id) AS reportCount, blur, b.artist, b.title
                FROM report_beatmap rb
                         JOIN beatmap b ON rb.beatmapset_id = b.beatmapset_id
                WHERE rb.report_type = 1
                GROUP BY rb.beatmapset_id, b.artist, b.title
                HAVING COUNT(DISTINCT rb.user_id) >= 5
                ORDER BY reportCount DESC;""", nativeQuery = true)
    List<BeatmapReportProjection> findMostReportedBeatmaps();
}
