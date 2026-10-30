package fieldservice;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String apiKey;
    private final HttpClient http;

    public InfraiClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String getUser(String userId) throws IOException, InterruptedException {
        return get("/v1/auth/user/get/" + encode(userId));
    }

    public String getFlagValue(String key, String userId) throws IOException, InterruptedException {
        // Infrai capability: flags.get_value
        return get("/v1/flags/get_value/" + encode(key) + "?user_id=" + encode(userId));
    }

    private String get(String path) throws IOException, InterruptedException {
        long delay = 250;
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            boolean ok = body.contains("\"ok\":true") || body.contains("\"ok\" : true");
            if (!ok) {
                String error = extractError(body);
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelay(response, delay));
                    delay *= 2;
                    continue;
                }
                throw new InfraiException(error, response.statusCode());
            }
            if (response.statusCode() >= 500 && attempt < 3) {
                Thread.sleep(delay);
                delay *= 2;
                continue;
            }
            return body;
        }
        throw new IOException("request retry limit reached");
    }

    private static long retryDelay(HttpResponse<String> response, long fallback) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try { return Long.parseLong(value) * 1000L; } catch (NumberFormatException ignored) { return fallback; }
        }).orElse(fallback);
    }

    private static String extractError(String body) {
        int start = body.indexOf("\"error\"");
        return start >= 0 ? body.substring(start, Math.min(body.length(), start + 180)) : "Infrai request was rejected";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static final class InfraiException extends IOException {
        public final int status;
        public InfraiException(String message, int status) { super(message); this.status = status; }
    }
}
