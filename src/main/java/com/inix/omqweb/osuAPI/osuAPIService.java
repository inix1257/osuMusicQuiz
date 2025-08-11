package com.inix.omqweb.osuAPI;

import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.BeatmapSearchDTO;
import com.inix.omqweb.Beatmap.BeatmapService;
import com.inix.omqweb.DTO.AccessTokenDTO;
import com.inix.omqweb.DTO.PlayerDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class osuAPIService {
    private final PlayerRepository playerRepository;
    private final BeatmapService beatmapService;
    private final CacheService<String, Player> cacheService;

    private final Logger logger = LoggerFactory.getLogger(osuAPIService.class);

    private String clientAccessToken;

    @Value("${osu.clientId}")
    private String clientId;

    @Value("${osu.clientSecret}")
    private String clientSecret;

    @Value("${osu.redirectUri}")
    private String redirectUri;

    @Value("${osu.apiKey}")
    private String apiKey;

    public ResponseEntity<AccessTokenDTO> getToken(@PathVariable String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept", "application/json");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.exchange("https://osu.ppy.sh/oauth/token", HttpMethod.POST, entity, AccessTokenDTO.class);
    }

    public AccessTokenDTO getRefreshToken(String refreshToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept", "application/json");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<AccessTokenDTO> response;

        try {
            response = restTemplate.exchange("https://osu.ppy.sh/oauth/token", HttpMethod.POST, entity, AccessTokenDTO.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Handle 401 error here
                return null;
            } else {
                throw e;
            }
        }

        return response.getBody();
    }

    public ResponseEntity<Player> getMeByToken(String token) {
        if (token == null) return null;

        Player cachedPlayer = cacheService.get(token);
        if (cachedPlayer != null) {
            return ResponseEntity.ok(cachedPlayer);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<PlayerDTO> response;

        try {
            response = restTemplate.exchange("https://osu.ppy.sh/api/v2/me", HttpMethod.GET, entity, PlayerDTO.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                // Handle 401 error here
                logger.error("Too many requests to osu API");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }

        PlayerDTO player = response.getBody();

        // Get player if exists, else create new player by builder
        Player dbPlayer = playerRepository.findById(player.getId()).orElse(
                Player.builder()
                        .playerStats(new HashMap<>())
                        .ban(false)
                        .points(BigDecimal.valueOf(0))
                        .build()
        );

        dbPlayer.setId(player.getId());
        dbPlayer.setUsername(player.getUsername());
        dbPlayer.setAvatar_url(player.getAvatar_url());
        dbPlayer.setCover_url(player.getCover_url());
        dbPlayer.setCountry_code(player.getCountry_code());
        dbPlayer.setRank(playerRepository.countByPointsGreaterThanAndBanFalse(dbPlayer.getPoints()) + 1);
        dbPlayer.setJoin_date(player.getJoin_date());

        if (dbPlayer.getRegister_date() == null) {
            logger.info("New player registered: " + dbPlayer.getUsername());
            dbPlayer.setRegister_date(java.sql.Timestamp.valueOf(LocalDateTime.now()));
        }

        cacheService.put(token, dbPlayer);

        playerRepository.save(dbPlayer);

        return ResponseEntity.ok(dbPlayer);
    }

    @PostConstruct
    public void getClientAccessToken() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "client_credentials");
        params.add("scope", "public");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept", "application/json");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<AccessTokenDTO> response = restTemplate.exchange("https://osu.ppy.sh/oauth/token", HttpMethod.POST, entity, AccessTokenDTO.class);

        clientAccessToken = response.getBody().getAccess_token();
    }

    private String cursor_string = "";
    // eyJmYXZvdXJpdGVfY291bnQiOjIyODUsImlkIjoxMzczOTR9 TAIKO
    // eyJmYXZvdXJpdGVfY291bnQiOjIzOSwiaWQiOjEyMjgyMTd9 CTB
    // eyJmYXZvdXJpdGVfY291bnQiOjY0MSwiaWQiOjE1NjM0MDd9 MANIA

//    @PostConstruct
//    @Scheduled(fixedRate = 3000)
    private void searchBeatmap() throws IOException, ParseException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Bearer " + clientAccessToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<BeatmapSearchDTO> response = restTemplate.exchange("https://osu.ppy.sh/api/v2/beatmapsets/search?m=3&sort=favourites_desc&cursor_string=" + cursor_string, HttpMethod.GET, entity, BeatmapSearchDTO.class);
        System.out.println("Response: " + response.getBody());
        List<Beatmap> beatmaps = response.getBody().getBeatmapsets();

        for (Beatmap beatmap : beatmaps) { //
            if (beatmap.getPlay_count() > 1000 * 500 || beatmap.getFavourite_count() > 300) {
                System.out.println("Play count: " + beatmap.getPlay_count() + " Favourite count: " + beatmap.getFavourite_count());
                beatmapService.addBeatmap(String.valueOf(beatmap.getId()));
            }
        }

        cursor_string = response.getBody().getCursor_string();
        System.out.println("Cursor string: " + cursor_string);
    }

    /**
     * Fetch beatmap statistics for a single beatmapset using osu! API v1
     * @param beatmapSetId The beatmapset ID to fetch
     * @return BeatmapStats object with play count and favourite count
     */
    public BeatmapStats fetchBeatmapStats(int beatmapSetId) {
        try {
            String url = String.format("https://osu.ppy.sh/api/get_beatmaps?k=%s&s=%d", apiKey, beatmapSetId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept", "application/json");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonResponse = mapper.readTree(response.getBody());
                
                if (jsonResponse.isArray() && jsonResponse.size() > 0) {
                    JsonNode beatmap = jsonResponse.get(0); // Get the first beatmap in the set
                    int playCount = beatmap.path("playcount").asInt(0);
                    int favouriteCount = beatmap.path("favourite_count").asInt(0);
                    
                    return new BeatmapStats(playCount, favouriteCount);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch beatmap stats for beatmapset {}: {}", beatmapSetId, e.getMessage());
        }
        
        return new BeatmapStats(0, 0); // Return default values if fetch fails
    }
    
    /**
     * Batch fetch beatmap statistics using osu! API v1
     * @param beatmapSetIds List of beatmap set IDs to fetch
     * @return Map of beatmap set ID to BeatmapStats
     */
    public Map<Integer, BeatmapStats> fetchBeatmapStatsBatch(List<Integer> beatmapSetIds) {
        Map<Integer, BeatmapStats> statsMap = new HashMap<>();
        
        logger.info("Fetching stats for {} beatmapsets using osu! API v1", beatmapSetIds.size());
        
        for (Integer beatmapSetId : beatmapSetIds) {
            try {
                BeatmapStats stats = fetchBeatmapStats(beatmapSetId);
                statsMap.put(beatmapSetId, stats);
                
                // Add a small delay to avoid overwhelming the API
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.error("Failed to fetch stats for beatmapset {}: {}", beatmapSetId, e.getMessage());
                statsMap.put(beatmapSetId, new BeatmapStats(0, 0)); // Default values
            }
        }
        
        return statsMap;
    }
    
    /**
     * Helper class to hold beatmap statistics
     */
    public static class BeatmapStats {
        private final int playCount;
        private final int favouriteCount;
        
        public BeatmapStats(int playCount, int favouriteCount) {
            this.playCount = playCount;
            this.favouriteCount = favouriteCount;
        }
        
        public int getPlayCount() {
            return playCount;
        }
        
        public int getFavouriteCount() {
            return favouriteCount;
        }
    }
}
