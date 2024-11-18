package com.inix.omqweb.osuAPI;

import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.BeatmapSearchDTO;
import com.inix.omqweb.Beatmap.BeatmapService;
import com.inix.omqweb.DTO.AccessTokenDTO;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class osuAPIService {
    private final PlayerRepository playerRepository;
    private final BeatmapService beatmapService;

    private final Logger logger = LoggerFactory.getLogger(osuAPIService.class);

    private String clientAccessToken;

    @Value("${osu.clientId}")
    private String clientId;

    @Value("${osu.clientSecret}")
    private String clientSecret;

    @Value("${osu.redirectUri}")
    private String redirectUri;

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

        ResponseEntity<AccessTokenDTO> response = restTemplate.exchange("https://osu.ppy.sh/oauth/token", HttpMethod.POST, entity, AccessTokenDTO.class);
        return response;
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

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Player> response;


        try {
            response = restTemplate.exchange("https://osu.ppy.sh/api/v2/me", HttpMethod.GET, entity, Player.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                // Handle 401 error here
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }

        // Get player if exists, else create new player by builder
        Player player = playerRepository.findById(response.getBody().getId()).orElse(
                Player.builder()
                        .ban(false)
                        .points(BigDecimal.valueOf(0))
                        .build()
        );
        player.setId(response.getBody().getId());
        player.setUsername(response.getBody().getUsername());
        player.setAvatar_url(response.getBody().getAvatar_url());
        player.setCover_url(response.getBody().getCover_url());
        player.setCountry_code(response.getBody().getCountry_code());
        player.setRank(playerRepository.countByPointsGreaterThanAndBanFalse(player.getPoints()) + 1);
        player.setJoin_date(response.getBody().getJoin_date());

        if (player.getRegister_date() == null) {
            logger.info("New player registered: " + player.getUsername());
            player.setRegister_date(java.sql.Timestamp.valueOf(LocalDateTime.now()));
        }

        playerRepository.save(player);

        return ResponseEntity.ok(player);
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

//    @Scheduled(fixedRate = 3000)
    private void searchBeatmap() throws IOException, ParseException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Bearer " + clientAccessToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<BeatmapSearchDTO> response = restTemplate.exchange("https://osu.ppy.sh/api/v2/beatmapsets/search?m=0&sort=plays_desc&cursor_string=" + cursor_string, HttpMethod.GET, entity, BeatmapSearchDTO.class);

        List<Beatmap> beatmaps = response.getBody().getBeatmapsets();

        for (Beatmap beatmap : beatmaps) {
            if (beatmap.getPlay_count() > 3000000 || beatmap.getFavourite_count() > 1000) {
                beatmapService.addBeatmap(String.valueOf(beatmap.getId()));
            }
        }

        cursor_string = response.getBody().getCursor_string();
        System.out.println("Cursor string: " + cursor_string);
    }
}
