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

public class MinecraftWikiTool implements AgentTool {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String API_BASE = "https://minecraft.wiki/api.php";
    private static final String USER_AGENT = "AiChatBuddy/1.2 (github.com/mrgucci1/AiChatBuddy)";
    private static final int MAX_EXTRACT_CHARS = 500;

    private final int maxResults;

    public MinecraftWikiTool(int maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public String name() { return "minecraft_wiki"; }

    @Override
    public String description() {
        return "Look up Minecraft game mechanics, crafting recipes, items, mobs, and other Minecraft-specific information from the official Minecraft Wiki.";
    }

    @Override
    public JSONObject parametersSchema() {
        return AgentTool.singleQuerySchema(
                "What to look up on the Minecraft Wiki (e.g. 'beacon crafting', 'creeper', 'redstone repeater')");
    }

    @Override
    public String execute(JSONObject args) throws IOException {
        String query = (String) args.get("query");
        if (query == null || query.isBlank()) return "error: no query provided";

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String searchUrl = API_BASE + "?action=query&list=search&srsearch=" + encoded
                + "&srlimit=" + maxResults + "&format=json";
        JSONObject searchResult = fetchJson(searchUrl);

        JSONObject queryObj = (JSONObject) searchResult.get("query");
        if (queryObj == null) return "No results found on Minecraft Wiki.";
        JSONArray searchResults = (JSONArray) queryObj.get("search");
        if (searchResults == null || searchResults.isEmpty()) return "No results found on Minecraft Wiki.";

        StringBuilder pageIds = new StringBuilder();
        for (Object r : searchResults) {
            JSONObject sr = (JSONObject) r;
            if (pageIds.length() > 0) pageIds.append("%7C");
            pageIds.append(sr.get("pageid"));
        }

        String extractUrl = API_BASE + "?action=query&prop=extracts&exintro=1&explaintext=1&pageids="
                + pageIds + "&format=json";
        JSONObject extractResult = fetchJson(extractUrl);

        JSONObject pages = (JSONObject) ((JSONObject) extractResult.get("query")).get("pages");
        if (pages == null || pages.isEmpty()) return "No content found on Minecraft Wiki.";

        StringBuilder sb = new StringBuilder();
        for (Object key : pages.keySet()) {
            JSONObject page = (JSONObject) pages.get(key);
            String title = (String) page.get("title");
            String extract = (String) page.get("extract");
            if (extract != null && !extract.isBlank()) {
                if (extract.length() > MAX_EXTRACT_CHARS) extract = extract.substring(0, MAX_EXTRACT_CHARS) + "...";
                sb.append(title).append(": ").append(extract.trim()).append("\n\n");
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : "No content found on Minecraft Wiki.";
    }

    private JSONObject fetchJson(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Minecraft Wiki request interrupted", e);
        }

        try {
            return (JSONObject) new JSONParser().parse(response.body());
        } catch (ParseException e) {
            throw new IOException("Failed to parse MediaWiki response: " + e.getMessage(), e);
        }
    }
}
