package com.inix.omqweb.Beatmap;

import com.inix.omqweb.Beatmap.Alias.Alias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeatmapPatternRepository extends JpaRepository<BeatmapPattern, Integer> {
    @Query("SELECT p FROM BeatmapPattern p WHERE p.beatmap.beatmapset_id IN :beatmapIds")
    List<BeatmapPattern> findAllByBeatmapIds(@Param("beatmapIds") List<Integer> beatmapIds);
}
