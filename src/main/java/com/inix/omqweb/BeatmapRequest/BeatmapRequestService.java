package com.inix.omqweb.BeatmapRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inix.omqweb.osuAPI.Player;
import com.inix.omqweb.Beatmap.BeatmapRepository;
import com.inix.omqweb.osuAPI.osuAPIService;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class BeatmapRequestService {
    private final BeatmapRequestRepository requestRepository;
    private final BeatmapRequestVoteRepository voteRepository;
    private final BeatmapRepository beatmapRepository;
    private final osuAPIService osuAPIService;
    @Value("${osu.apiKey}")
    private String apiKey;
    
    private final Logger logger = LoggerFactory.getLogger(BeatmapRequestService.class);

    public List<BeatmapRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public int getVoteCount(Long requestId) {
        return voteRepository.countByRequestId(requestId);
    }

    public boolean hasUserRequested(int beatmapSetId, String submitterId) {
        return requestRepository.findByBeatmapSetIdAndSubmitterId(beatmapSetId, submitterId).isPresent();
    }

    @Transactional
    public BeatmapRequest submitRequest(int beatmapSetId, Player submitter) throws IOException, ParseException {
        // Check if beatmap already exists in the database
        if (beatmapRepository.findById(beatmapSetId).isPresent()) {
            throw new IllegalArgumentException("This beatmap already exists in the database.");
        }
        if (hasUserRequested(beatmapSetId, submitter.getId())) {
            throw new IllegalArgumentException("You have already requested this beatmap.");
        }

        // Fetch beatmap metadata from osu! API
        Map<String, String> beatmapInfo = fetchBeatmapInfoFromAPI(beatmapSetId);
        
        int approved = Integer.parseInt(beatmapInfo.get("approved"));
        if(approved <= 0 || approved == 2) {
            throw new IllegalArgumentException("Beatmap needs to be ranked/loved.");
        }

        BeatmapRequest request = BeatmapRequest.builder()
                .beatmapSetId(beatmapSetId)
                .submitterId(submitter.getId())
                .submittedAt(new Timestamp(System.currentTimeMillis()))
                .status(BeatmapRequest.Status.PENDING)
                .artist(beatmapInfo.get("artist"))
                .title(beatmapInfo.get("title"))
                .creator(beatmapInfo.get("creator"))
                .rankedDate(parseDate(beatmapInfo.get("approved_date")))
                .build();
        
        BeatmapRequest savedRequest = requestRepository.save(request);
        
        // Automatically vote for the submitter
        BeatmapRequestVote autoVote = BeatmapRequestVote.builder()
                .request(savedRequest)
                .voterId(submitter.getId())
                .votedAt(new Timestamp(System.currentTimeMillis()))
                .build();
        voteRepository.save(autoVote);
        
        return savedRequest;
    }

    public Map<String, String> fetchBeatmapInfoFromAPI(int beatmapSetId) throws IOException {
        URL url = new URL("https://osu.ppy.sh/api/get_beatmaps?k=" + apiKey + "&s=" + beatmapSetId);
        BufferedReader bf;
        String line;
        String result = "";
        bf = new BufferedReader(new InputStreamReader(url.openStream()));

        while ((line = bf.readLine()) != null) {
            result = result.concat(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> jsonList = mapper.readValue(result, new TypeReference<List<Map<String, String>>>() {});
        
        if (jsonList.isEmpty()) {
            throw new IllegalArgumentException("Beatmap not found or not accessible.");
        }

        return jsonList.get(0); // Return the first beatmap in the set
    }

    private Timestamp parseDate(String dateString) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return new Timestamp(dateFormat.parse(dateString).getTime());
    }

    public List<BeatmapRequestVote> getVotesForRequest(Long requestId) {
        return voteRepository.findByRequestId(requestId);
    }

    public boolean hasUserVoted(Long requestId, String voterId) {
        return voteRepository.findByRequestIdAndVoterId(requestId, voterId).isPresent();
    }

    @Transactional
    public BeatmapRequestVote vote(Long requestId, Player voter) {
        if (hasUserVoted(requestId, voter.getId())) {
            throw new IllegalArgumentException("You have already voted for this request.");
        }
        BeatmapRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        BeatmapRequestVote vote = BeatmapRequestVote.builder()
                .request(request)
                .voterId(voter.getId())
                .votedAt(new Timestamp(System.currentTimeMillis()))
                .build();
        return voteRepository.save(vote);
    }

    @Transactional
    public void deleteRequestsByBeatmapSetId(int beatmapSetId) {
        List<BeatmapRequest> requests = requestRepository.findByBeatmapSetId(beatmapSetId);
        
        for (BeatmapRequest request : requests) {
            // Delete all votes for this request first
            List<BeatmapRequestVote> votes = voteRepository.findByRequestId(request.getId());
            voteRepository.deleteAll(votes);
            
            // Delete the request
            requestRepository.delete(request);
        }
    }

    /**
     * Scheduled method to update playcounts and favourite counts for all beatmaps in request listing
     * Uses osu! API v1 to fetch individual beatmapsets
     * Runs every 6 hours to avoid hitting API rate limits
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 6) // Every 6 hours
    @PostConstruct
    public void updateBeatmapRequestStats() {
        logger.info("Starting scheduled update of beatmap request statistics...");
        
        List<BeatmapRequest> allRequests = requestRepository.findAll();
        if (allRequests.isEmpty()) {
            logger.info("No beatmap requests found to update");
            return;
        }
        
        // Extract unique beatmap set IDs
        List<Integer> beatmapSetIds = allRequests.stream()
                .map(BeatmapRequest::getBeatmapSetId)
                .distinct()
                .collect(Collectors.toList());
        
        logger.info("Updating stats for {} unique beatmapsets", beatmapSetIds.size());
        
        try {
            // Fetch beatmap data using osu! API v1
            Map<Integer, osuAPIService.BeatmapStats> beatmapStatsMap = osuAPIService.fetchBeatmapStatsBatch(beatmapSetIds);
            
            int updatedCount = 0;
            int errorCount = 0;
            
            for (BeatmapRequest request : allRequests) {
                try {
                    osuAPIService.BeatmapStats stats = beatmapStatsMap.get(request.getBeatmapSetId());
                    if (stats != null) {
                        // Update the request if values have changed
                        boolean needsUpdate = false;
                        if (request.getPlayCount() != stats.getPlayCount()) {
                            request.setPlayCount(stats.getPlayCount());
                            needsUpdate = true;
                        }
                        if (request.getFavouriteCount() != stats.getFavouriteCount()) {
                            request.setFavouriteCount(stats.getFavouriteCount());
                            needsUpdate = true;
                        }
                        
                        if (needsUpdate) {
                            requestRepository.save(request);
                            updatedCount++;
                            logger.debug("Updated stats for beatmap request {}: playCount={}, favouriteCount={}", 
                                        request.getBeatmapSetId(), stats.getPlayCount(), stats.getFavouriteCount());
                        }
                    } else {
                        logger.warn("No stats found for beatmap set ID: {}", request.getBeatmapSetId());
                        errorCount++;
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Failed to update stats for beatmap request {}: {}", 
                               request.getBeatmapSetId(), e.getMessage());
                }
            }
            
            logger.info("Completed beatmap request stats update: {} updated, {} errors out of {} total requests", 
                       updatedCount, errorCount, allRequests.size());
            
        } catch (Exception e) {
            logger.error("Failed to fetch beatmap stats: {}", e.getMessage());
        }
    }
}
