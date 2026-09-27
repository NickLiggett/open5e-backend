package com.main.app.importer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads default content from the Open5e v2 API ({@code open5e.import.source-url}, by default the public API). Each
 * endpoint is paged; this follows {@code next} until the end.
 */
@Component
public class Open5eApiSource implements DefaultContentSource {

    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 1000;

    private final RestClient http;
    private final String baseUrl;

    public Open5eApiSource(@Value("${open5e.import.source-url:https://api.open5e.com/v2}") String baseUrl) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
        requests.setReadTimeout(Duration.ofMinutes(2));
        this.http = RestClient.builder().requestFactory(requests).build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public List<ObjectNode> fetch(String endpoint) {
        List<ObjectNode> rows = new ArrayList<>();
        String next = baseUrl + "/" + endpoint + "/?format=json&limit=" + PAGE_SIZE;
        for (int page = 0; next != null; page++) {
            if (page == MAX_PAGES) {
                throw new IllegalStateException("Too many pages from " + endpoint);
            }
            JsonNode body = http.get().uri(URI.create(next)).retrieve().body(JsonNode.class);
            if (body == null || !body.path("results").isArray()) {
                throw new IllegalStateException("Unexpected response from " + next);
            }
            body.get("results").forEach(row -> rows.add((ObjectNode) row));
            next = body.path("next").isString() ? body.get("next").asString() : null;
        }
        return rows;
    }

    @Override
    public String describe() {
        return baseUrl;
    }
}
