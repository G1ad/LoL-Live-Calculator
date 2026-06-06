package org.lollivecalculator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class DataDownloader {

    private static final String CHAMPIONS_URL = "http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/champions.json";
    private static final String ITEMS_URL = "http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/items.json";

    private final HttpClient httpClient;

    public DataDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<Path> downloadJsonAsync(String urlString, String targetFilename) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        Path outputPath = Paths.get(targetFilename);

        // Send async request and pipe the body directly to a file stream
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(outputPath))
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        throw new RuntimeException("Failed to download file. HTTP Status: " + response.statusCode());
                    }
                });
    }

    public CompletableFuture<Path> downloadChampionsAsync() {
        return downloadJsonAsync(CHAMPIONS_URL, "champions.json");
    }

    public CompletableFuture<Path> downloadItemsAsync() {
        return downloadJsonAsync(ITEMS_URL, "items.json");
    }
}