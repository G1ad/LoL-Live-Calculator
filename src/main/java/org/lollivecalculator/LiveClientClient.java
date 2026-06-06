package org.lollivecalculator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class LiveClientClient {

    private static final String LIVE_DATA_URL = "https://127.0.0.1:2999/liveclientdata/allgamedata";
    private final HttpClient httpClient;

    public LiveClientClient() {
        this.httpClient = HttpClient.newBuilder()
                .sslContext(createInsecureSslContext())
                .build();
    }

    private SSLContext createInsecureSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure insecure SSL Context", e);
        }
    }

    public CompletableFuture<String> fetchLiveGameDataAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LIVE_DATA_URL))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        throw new RuntimeException("HTTP " + response.statusCode());
                    }
                });
    }
}