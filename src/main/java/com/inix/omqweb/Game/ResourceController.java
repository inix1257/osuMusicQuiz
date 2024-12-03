package com.inix.omqweb.Game;

import com.inix.omqweb.Util.AESUtil;
import com.inix.omqweb.osuAPI.Player;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
public class ResourceController {
    private final AESUtil aesUtil;

    @GetMapping("/audio/{encryptedId}")
    public Mono<ResponseEntity<Resource>> getAudio(@PathVariable String encryptedId) {
        String id = aesUtil.decrypt(encryptedId);
        Path path = Path.of("./preview/" + id + ".mp3");

        if (Files.exists(path)) {
            Resource resource = new FileSystemResource(path);
            return Mono.just(
                    ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource)
            );
        } else {
            throw new RuntimeException("File not found: " + path);
        }

    }

    @GetMapping("/image/{encryptedId}")
    public Mono<ResponseEntity<Resource>> getImage(@PathVariable String encryptedId) {
        String id;
        id = aesUtil.decrypt(encryptedId);

        Path path = Path.of("./preview/" + id + ".jpg");

        if (Files.exists(path)) {
            Resource resource = new FileSystemResource(path);
            return Mono.just(
                    ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(resource)
            );
        } else {
            throw new RuntimeException("File not found: " + path);
        }

    }

    @GetMapping("/image/{encryptedId}/{encryptedKey}")
    public Mono<ResponseEntity<Resource>> getOriginalImage(@PathVariable String encryptedId, @PathVariable String encryptedKey) {
        String id;
        String key;

        id = aesUtil.decrypt(encryptedId);
        key = aesUtil.decrypt(encryptedKey);

        Path path = Path.of("./preview/" + id + "_original.jpg");

        if (Files.exists(path)) {
            Resource resource = new FileSystemResource(path);
            return Mono.just(
                    ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(resource)
            );
        } else {
            throw new RuntimeException("File not found: " + path);
        }

    }

    @GetMapping("/beatmap/{id}")
    public String getBeatmap(@PathVariable String id) {
        StringBuilder textData = new StringBuilder();

        String urlString = "https://osu.ppy.sh/osu/" + id;

        try {
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    textData.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return textData.toString();
    }
}