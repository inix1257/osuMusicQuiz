package com.inix.omqweb.Discord;

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

    @Value("${discord.webhook.url}")
    private String webhookUrl;

    public void sendWebhook(String message) {
        // Send message to Discord webhook
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = new HashMap<>();
        payload.put("username", "OMQ Log");
        payload.put("content", message);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);
    }
}
