package org.lollivecalculator.network;

import org.lollivecalculator.config.AppConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;

public class LiveClientClient {

    private static final AppConfig CFG = AppConfig.getInstance();

    private final HttpClient httpClient;
    private final String liveDataUrl;

    public LiveClientClient() {
        this.liveDataUrl = CFG.getLolApiUrl();
        this.httpClient = buildPermissiveHttpClient();
    }

    /**
     * Builds an HttpClient that bypasses SSL certificate verification and
     * hostname checking.
     * <p>
     * The LoL Live Client API uses a self-signed certificate on 127.0.0.1:2999.
     * Without both of these bypasses, Java would reject the connection with
     * either an SSLHandshakeException (untrusted cert) or an
     * SSLPeerUnverifiedException (hostname mismatch).
     * </p>
     */
    private static HttpClient buildPermissiveHttpClient() {
        try {
            // Step 1: Create SSLContext that trusts ALL certificates
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // Trust all client certificates
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // Trust all server certificates
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Step 2: Disable hostname verification via SSLParameters.
            // This bypasses the hostname check that would otherwise fail
            // when the self-signed cert's CN doesn't match "127.0.0.1".
            // HttpClient.Builder does NOT have a hostnameVerifier() method
            // in earlier JDK versions; the portable way is SSLParameters.
            javax.net.ssl.SSLParameters params = sslContext.getDefaultSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);  // Disable hostname check

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(params)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create permissive HTTP client", e);
        }
    }

    public CompletableFuture<String> fetchLiveGameDataAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(liveDataUrl))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    }
                    throw new RuntimeException("HTTP " + response.statusCode());
                });
    }
}