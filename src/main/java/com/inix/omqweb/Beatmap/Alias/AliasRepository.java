package com.inix.omqweb.Beatmap.Alias;

import com.inix.omqweb.Beatmap.Beatmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AliasRepository extends JpaRepository<Alias, AliasId> {
    @Query("SELECT a FROM Alias a WHERE a.beatmap = :beatmap AND a.aliasType = :aliasType")
    List<Alias> findAllByBeatmapAndAliasType(@Param("beatmap") Beatmap beatmap, @Param("aliasType") AliasType aliasType);

    @Query("SELECT a FROM Alias a WHERE a.beatmap.beatmapset_id IN :beatmapIds")
    List<Alias> findAllByBeatmapIds(@Param("beatmapIds") List<Integer> beatmapIds);
}
