package com.inix.omqweb.BeatmapReport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeatmapReportRepository extends JpaRepository<BeatmapReport, Integer> {
    @Query(value = "SELECT * FROM report_beatmap WHERE beatmapset_id = :beatmapsetId", nativeQuery = true)
    List<BeatmapReport> findAllByBeatmapsetId(int beatmapsetId);


}
