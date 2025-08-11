package com.inix.omqweb.BeatmapRequest;

import com.inix.omqweb.Beatmap.BeatmapRepository;
import com.inix.omqweb.osuAPI.Player;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.text.ParseException;

@RestController
@RequestMapping("/api/beatmap-request")
@RequiredArgsConstructor
public class BeatmapRequestController {
    private final BeatmapRequestService service;
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRequests() {
        List<BeatmapRequest> requests = service.getAllRequests();
        List<Map<String, Object>> result = requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", req.getId());
            map.put("beatmapSetId", req.getBeatmapSetId());
            map.put("submitterId", req.getSubmitterId());
            map.put("submittedAt", req.getSubmittedAt());
            map.put("status", req.getStatus());
            map.put("votes", service.getVoteCount(req.getId()));
            
            // Use stored metadata
            map.put("title", req.getTitle());
            map.put("artist", req.getArtist());
            map.put("creator", req.getCreator());
            map.put("rankedDate", req.getRankedDate());
            map.put("playCount", req.getPlayCount());
            map.put("favouriteCount", req.getFavouriteCount());
            
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> submitRequest(@RequestParam int beatmapSetId, HttpServletRequest request) throws IOException, ParseException {
        Player player = (Player) request.getAttribute("userInfo");
        try {
            BeatmapRequest req = service.submitRequest(beatmapSetId, player);
            return ResponseEntity.ok(req);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<?> vote(@PathVariable Long id, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");
        try {
            BeatmapRequestVote vote = service.vote(id, player);
            return ResponseEntity.ok(Map.of("success", true, "voteId", vote.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
