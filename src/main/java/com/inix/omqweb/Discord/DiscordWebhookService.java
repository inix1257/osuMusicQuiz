package com.inix.omqweb.Discord;

import com.inix.omqweb.Util.ProfileUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscordWebhookService {
    private final ProfileUtil profileUtil;

    @Value("${discord.webhook.lobby.url}")
    private String webhookLobbyUrl;

    @Value("${discord.webhook.beatmap.url}")
    private String webhookBeatmapUrl;

    public void sendWebhook(String url, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = new HashMap<>();
        if (profileUtil.isDevEnv()) {
            payload.put("username", "OMQ Log (Dev Server)");
        } else {
            payload.put("username", "OMQ Log");
        }
        payload.put("content", message);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public void sendLobbyWebhook(String message) {
        sendWebhook(webhookLobbyUrl, message);
    }

    public void sendBeatmapWebhook(String message) {
        sendWebhook(webhookBeatmapUrl, message);
    }
}
