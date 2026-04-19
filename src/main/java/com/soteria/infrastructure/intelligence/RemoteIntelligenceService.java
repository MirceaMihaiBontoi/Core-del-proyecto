package com.soteria.infrastructure.intelligence;

import com.soteria.core.interfaces.EmergencyClassifier;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementation of EmergencyClassifier that connects to a remote AI service.
 * Includes circuit breaking and retry logic for robustness.
 */
public class RemoteIntelligenceService implements EmergencyClassifier {

    private static final Logger log = Logger.getLogger(RemoteIntelligenceService.class.getName());

    private static final int    MAX_RETRIES             = 3;
    private static final long   BACKOFF_BASE_MS         = 500;
    private static final int    CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final Duration CIRCUIT_OPEN_TTL      = Duration.ofSeconds(60);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker = new CircuitBreaker();

    public RemoteIntelligenceService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // -- EmergencyClassifier Implementation ------------------------------------

    @Override
    public String classify(String text) {
        String body = "{\"text\": \"" + escapeJson(text) + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/classify"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();
        return sendWithRetry(req);
    }

    @Override
    public boolean isAvailable() {
        if (circuitBreaker.isOpen()) return false;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                circuitBreaker.recordSuccess();
                return true;
            }
            circuitBreaker.recordFailure();
            return false;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            return false;
        }
    }

    // -- Extended Intelligence Features ---------------------------------------

    public String chat(String message, String context) {
        String body = "{\"message\": \"" + escapeJson(message) +
                      "\", \"context\": \"" + escapeJson(context) + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat"))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
        return sendWithRetry(req);
    }

    public String geolocate() {
        if (circuitBreaker.isOpen()) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/geolocate"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && !resp.body().contains("\"error\"")) return resp.body();
            return null;
        } catch (Exception e) {
            log.warning("Geolocate error: " + e.getMessage());
            return null;
        }
    }

    public byte[] synthesize(String text, String emotion) {
        if (circuitBreaker.isOpen()) return null;
        try {
            String body = "{\"text\": \"" + escapeJson(text) +
                          "\", \"emotion\": \"" + escapeJson(emotion) + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/tts"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200) {
                String ct = resp.headers().firstValue("content-type").orElse("");
                if (ct.contains("application/json")) {
                    log.warning("TTS returned JSON instead of WAV: " +
                            new String(resp.body(), StandardCharsets.UTF_8));
                    return null;
                }
                return resp.body();
            }
            log.warning("TTS HTTP " + resp.statusCode());
            return null;
        } catch (Exception e) {
            log.warning("TTS Error: " + e.getMessage());
            return null;
        }
    }

    // -- Retry + Circuit Breaker Logic -----------------------------------------

    private String sendWithRetry(HttpRequest request) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            if (circuitBreaker.isOpen()) {
                log.warning("Circuit breaker OPEN - skipping backend call");
                return null;
            }
            try {
                HttpResponse<String> resp = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (resp.statusCode() == 200) {
                    circuitBreaker.recordSuccess();
                    return resp.body();
                }
                if (resp.statusCode() >= 400 && resp.statusCode() < 500) {
                    log.warning("HTTP " + resp.statusCode() + " (4xx) - no retry");
                    return null;
                }
                log.warning("HTTP " + resp.statusCode() + " (5xx), attempt " + (attempt + 1) + "/" + MAX_RETRIES);
                circuitBreaker.recordFailure();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (IOException e) {
                log.warning("IOException contacting backend: " + e.getMessage() +
                            " - attempt " + (attempt + 1) + "/" + MAX_RETRIES);
                circuitBreaker.recordFailure();
            }

            if (attempt < MAX_RETRIES - 1) {
                try {
                    Thread.sleep(BACKOFF_BASE_MS * (1L << attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private class CircuitBreaker {
        private int consecutiveFailures = 0;
        private Instant openedAt = Instant.EPOCH;
        private boolean open = false;

        synchronized boolean isOpen() {
            if (!open) return false;
            if (Duration.between(openedAt, Instant.now()).compareTo(CIRCUIT_OPEN_TTL) > 0) {
                open = false;
                consecutiveFailures = 0;
            }
            return open;
        }

        synchronized void recordSuccess() {
            consecutiveFailures = 0;
            open = false;
        }

        synchronized void recordFailure() {
            consecutiveFailures++;
            if (consecutiveFailures >= CIRCUIT_FAILURE_THRESHOLD) {
                open = true;
                openedAt = Instant.now();
                log.warning("Circuit breaker OPENED after " + consecutiveFailures + " consecutive failures");
            }
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // Static extractors for JSON handling (manual to avoid dependencies)
    public static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
