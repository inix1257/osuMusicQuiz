package com.inix.omqweb.Game;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ResourceService {
    @Autowired
    private final WebClient webClient;

    public void getAudio(int beatmapsetId) {
        String audioUrl = "https://b.ppy.sh/preview/" + beatmapsetId + ".mp3";
        Path path = Path.of("./preview/" + beatmapsetId + ".mp3");
        fetchAndSaveAudio(audioUrl, path);
    }

    public void getImage(int beatmapsetId, boolean blur) {
        String imageUrl = "https://assets.ppy.sh/beatmaps/" + beatmapsetId + "/covers/raw.jpg";
        Path path = Path.of("./preview/" + beatmapsetId + ".jpg");
        fetchAndSaveImage(imageUrl, path, blur);
    }

    @Async
    public void getAudioAsync(int beatmapsetId) {
        getAudio(beatmapsetId);
    }

    @Async
    public void getImageAsync(int beatmapsetId, boolean blur) {
        getImage(beatmapsetId, blur);
    }


    private void fetchAndSaveAudio(String url, Path path) {
        try {
            if (!Files.exists(path)) {
                webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(data -> {
                            try {
                                // Write the data to a file
                                Files.write(path, data, StandardOpenOption.CREATE);
                            } catch (Exception e) {
                                throw new RuntimeException();
                            }
                        })
                        .block();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch the audio resource: " + url);
        }
    }

    private void fetchAndSaveImage(String url, Path path, boolean blur) {
        try {
            if (!Files.exists(path)) {
                webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(data -> {
                            try {
                                if (blur) {
                                    // Process the image data to create a blurred, downscaled version
                                    byte[] blurredData = imageProcess(data, true);
                                    // Write the blurred image data to the main file
                                    Files.write(path, blurredData, StandardOpenOption.CREATE);

                                    // Process the image data to create a downscaled version
                                    byte[] downscaledData = imageProcess(data, false);
                                    // Write the downscaled image data to a file with a postfix
                                    Path downscaledPath = Path.of(path.toString().replace(".jpg", "_original.jpg"));
                                    Files.write(downscaledPath, downscaledData, StandardOpenOption.CREATE);
                                } else {
                                    // Process the image data to create a downscaled version
                                    byte[] downscaledData = imageProcess(data, false);
                                    // Write the downscaled image data to the main file
                                    Files.write(path, downscaledData, StandardOpenOption.CREATE);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException();
                            }
                        })
                        .onErrorResume(e -> {
                            // Create a black image and save it
                            BufferedImage blackImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
                            try {
                                ImageIO.write(blackImage, "jpg", Files.newOutputStream(path));
                            } catch (IOException ioException) {
                                throw new RuntimeException("Failed to create a black image", ioException);
                            }
                            return Mono.empty();
                        })
                        .block();
            }
        } catch (Exception e) {

            throw new RuntimeException("Failed to fetch the image resource: " + url);
        }
    }

    private byte[] imageProcess(byte[] originalData, boolean applyBlur) {
        try {
            // Read the original image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalData));

            // Calculate the new dimensions while maintaining the original aspect ratio
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            double aspectRatio = (double) originalWidth / originalHeight;
            int newWidth = 960; // Desired width
            int newHeight = (int) (newWidth / aspectRatio); // Calculated height

            // Create a new image with the new dimensions
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            // Apply the blur effect if applyBlur is true
            if (applyBlur) {
                int blurLevel = 50; // Desired blur level

                // Define a kernel that blurs an image
                float[] blurKernel = new float[blurLevel * blurLevel];
                Arrays.fill(blurKernel, 1.0f / (blurLevel * blurLevel));

                // Create a ConvolveOp object with the blur kernel
                ConvolveOp blurOp = new ConvolveOp(new Kernel(blurLevel, blurLevel, blurKernel));

                // Apply the blur effect to the resized image
                resizedImage = blurOp.filter(resizedImage, null);
            }

            // Write the resized image to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resize and blur the image", e);
        }
    }
}
