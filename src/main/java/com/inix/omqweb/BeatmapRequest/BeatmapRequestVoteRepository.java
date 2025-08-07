package com.inix.omqweb.BeatmapRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface BeatmapRequestVoteRepository extends JpaRepository<BeatmapRequestVote, Long> {
    Optional<BeatmapRequestVote> findByRequestIdAndVoterId(Long requestId, String voterId);
    int countByRequestId(Long requestId);
    List<BeatmapRequestVote> findByRequestId(Long requestId);
}
