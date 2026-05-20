package io.github.mrgucci1.aiChatBuddy.tools;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

public class BraveSearchTool implements AgentTool {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final String apiKey;
    private final int maxResults;
    private final Logger logger;

    public BraveSearchTool(String apiKey, int maxResults, Logger logger) {
        this.apiKey = apiKey;
        this.maxResults = maxResults;
        this.logger = logger;
    }

    @Override
    public String name() { return "web_search"; }

    @Override
    public String description() {
        return "Search the web for current information. Use this for recent events, news, or anything that may have changed.";
    }

    @Override
    public JSONObject parametersSchema() {
        return AgentTool.singleQuerySchema("The search query");
    }

    @Override
    public String execute(JSONObject args) throws IOException {
        String query = (String) args.get("query");
        if (query == null || query.isBlank()) return "error: no query provided";

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.search.brave.com/res/v1/web/search?q=" + encoded + "&count=" + maxResults;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("X-Subscription-Token", apiKey)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Brave Search request interrupted", e);
        }

        String body = response.body();
        if (response.statusCode() != 200) {
            String preview = body == null ? "" : body.substring(0, Math.min(body.length(), 300));
            throw new IOException("Brave Search API error " + response.statusCode() + ": " + preview);
        }

        try {
            JSONObject json = (JSONObject) new JSONParser().parse(body);
            JSONObject web = (JSONObject) json.get("web");
            if (web == null) return "No results found.";
            JSONArray results = (JSONArray) web.get("results");
            if (results == null || results.isEmpty()) return "No results found.";

            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (Object r : results) {
                JSONObject result = (JSONObject) r;
                String title = (String) result.get("title");
                String desc = (String) result.get("description");
                String resultUrl = (String) result.get("url");
                if (desc != null && desc.length() > 200) desc = desc.substring(0, 200) + "...";
                sb.append(i++).append(". ").append(title).append(" — ").append(desc)
                        .append(" (").append(resultUrl).append(")\n");
            }
            return sb.toString().trim();
        } catch (ParseException e) {
            String preview = body == null ? "<null>" : body.substring(0, Math.min(body.length(), 300));
            throw new IOException("Failed to parse Brave Search response (status " + response.statusCode()
                    + "): " + preview, e);
        }
    }
}
