package org.lollivecalculator.network;

import org.lollivecalculator.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DataDownloader {

    private static final AppConfig CFG = AppConfig.getInstance();

    private final HttpClient httpClient;

    public DataDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads a JSON file and saves it to the configured data directory,
     * creating the directory if necessary.
     */
    public CompletableFuture<Path> downloadJsonAsync(String urlString, Path outputPath) {
        // Ensure the parent directory exists
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(outputPath))
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        throw new RuntimeException(
                                "Failed to download " + urlString + " — HTTP " + response.statusCode());
                    }
                });
    }

    public CompletableFuture<Path> downloadChampionsAsync() {
        return downloadJsonAsync(CFG.getChampionsUrl(), CFG.getChampionsLocalPath());
    }

    public CompletableFuture<Path> downloadItemsAsync() {
        return downloadJsonAsync(CFG.getItemsUrl(), CFG.getItemsLocalPath());
    }
}