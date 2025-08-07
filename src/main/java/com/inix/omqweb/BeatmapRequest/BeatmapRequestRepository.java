package com.inix.omqweb.BeatmapRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface BeatmapRequestRepository extends JpaRepository<BeatmapRequest, Long> {
    Optional<BeatmapRequest> findByBeatmapSetIdAndSubmitterId(int beatmapSetId, String submitterId);
    List<BeatmapRequest> findByBeatmapSetId(int beatmapSetId);
}
