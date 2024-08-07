package io.github.mrgucci1.aiChatBuddy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;

public class AiChatBuddy extends JavaPlugin implements Listener {

    private String apiKey;
    private String promptTemplate;
    private String apiUrl;

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (message.startsWith("!ask")) {
            String question = message.substring(4).trim();
            getLogger().info("Asking question: " + question);
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                String answer = queryGemini(question); // Call the integrated function
                getServer().getScheduler().runTask(this, () -> {
                    event.getPlayer().sendMessage(answer);
                });
            });
        }
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();  // Saves the default config.yml if it doesn't exist
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AiChatBuddy has been Enabled.");

    }

    @Override
    public void onDisable() {
        {
            getLogger().info("AiChatBuddy has been disabled.");
        }
    }

    private void loadConfig() {
        apiKey = getConfig().getString("api-key");
        promptTemplate = getConfig().getString("prompt-template", "");
        apiUrl = getConfig().getString("api-url", "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"); // Default URL

        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("API key not found in config.yml. Please configure it.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private String queryGemini(String question) {
        try {
            // Construct the request payload (using helper classes)
            String prompt = String.format(promptTemplate, question);
            QueryRequest requestData = new QueryRequest(new Content[] { new Content(new Part[] { new Part(prompt) }) });
            getLogger().info("Formatted Prompt: " + prompt);
            // Convert the payload to JSON
            Gson gson = new Gson();
            String jsonPayload = gson.toJson(requestData);

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            // Handle the response
            if (response.statusCode() == 200) {
                QueryResponse queryResponse = gson.fromJson(response.body(), QueryResponse.class);

                // 1. Check for Null Response:
                if (queryResponse == null) {
                    return "Gemini Pro did not return a valid response.";
                }

                // 2. Check for Empty Candidates Array:
                if (queryResponse.candidates == null || queryResponse.candidates.length == 0) {
                    return "Gemini Pro did not provide an answer.";
                }

                // 3. Check for Null Output:
                if (queryResponse.candidates[0].output == null) {
                    return "Gemini Pro's response is missing the output.";
                }

                // 4. Return the Output (only if not null):
                return queryResponse.candidates[0].output;

            } else {
                getLogger().warning("Gemini API Error: " + response.statusCode());
                return "Error communicating with Gemini. (Error code: " + response.statusCode() + ")";
            }
        } catch (Exception e) {
            getLogger().warning("Error querying Gemini API: " + e.getMessage());
            return "An error occurred while contacting Gemini.";
        }
    }

    // Helper classes for request and response
    static class QueryRequest {
        Content[] contents;

        public QueryRequest(Content[] contents) {
            this.contents = contents;
        }
    }

    static class Content {
        Part[] parts;

        public Content(Part[] parts) {
            this.parts = parts;
        }
    }

    static class Part {
        String text;

        public Part(String text) {
            this.text = text;
        }
    }

    static class QueryResponse {
        Candidate[] candidates;

        static class Candidate {
            String output;
        }
    }
}
