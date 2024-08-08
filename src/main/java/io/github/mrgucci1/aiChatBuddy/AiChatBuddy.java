package io.github.mrgucci1.aiChatBuddy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AiChatBuddy extends JavaPlugin implements Listener {

    private String apiKey;
    private String promptTemplate;
    private String apiUrl;
    private String botName;
    private boolean privateMessages;

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (message.startsWith("!ask")) {
            String question = message.substring(4).trim();
            getLogger().info("Asking question: " + question);
            if (privateMessages) {
                event.setCancelled(true);
            }
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                String answer = queryGemini(question); // Call the integrated function
                getServer().getScheduler().runTask(this, () -> {
                    String coloredMessage = String.format("\n§c[%s]§r %s", botName, answer);
                    if (privateMessages) {
                        event.getPlayer().sendMessage(answer);
                    }
                    else {
                        getServer().broadcastMessage(coloredMessage);
                    }
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
        promptTemplate = getConfig().getString("prompt-template", "You are an AI assistant responding to questions within a Minecraft chat environment. \n" +
                "Please keep your answers concise, informative, and relevant to the context. \n" +
                "Do not include images, links, or any other content that cannot be displayed in chat.\n" +
                "Question: %s");
        apiUrl = getConfig().getString("api-url", "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"); // Default URL
        botName = getConfig().getString("bot-name", "Notch");
        privateMessages =  getConfig().getBoolean("private-questions", false);

        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("API key not found in config.yml. Please configure it.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private String queryGemini(String question) {
        try {
            // Build the request body
            String prompt = String.format(promptTemplate, question);
            String requestBody = getPromptBody(prompt);

            // Build the HTTP request (no RestTemplate)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the request and get the response (using java.net.http.HttpClient)
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            // Handle the response based on the status code
            if (response.statusCode() == 200) {
                String responseText = response.body();
                try {
                    responseText = parseGeminiResponse(responseText);
                } catch (ParseException e) {
                    getLogger().warning("Error parsing Gemini response: " + e.getMessage());
                }
                return responseText;
            } else {
                getLogger().warning("Gemini API Error: " + response.statusCode());
                return "Error communicating with Gemini. (Error code: " + response.statusCode() + ")";
            }
        } catch (IOException | InterruptedException e) {
            getLogger().warning("Error querying Gemini API: " + e.getMessage());
            return "An error occurred while contacting Gemini.";
        }
    }

    public String getPromptBody(String prompt) {
        // Create prompt for generating summary in document language
        JSONObject promptJson = new JSONObject();

        // Array to contain all the content-related data, including the text and role
        JSONArray contentsArray = new JSONArray();
        JSONObject contentsObject = new JSONObject();
        contentsObject.put("role", "user");

        // Array to hold the specific parts (or sections) of the user's input text
        JSONArray partsArray = new JSONArray();
        JSONObject partsObject = new JSONObject();
        partsObject.put("text", prompt);
        partsArray.add(partsObject);
        contentsObject.put("parts", partsArray);

        contentsArray.add(contentsObject);
        promptJson.put("contents", contentsArray);

        // Array to hold various safety setting objects to ensure the content is safe and appropriate
        JSONArray safetySettingsArray = new JSONArray();

        // Creating and setting generation configuration parameters such as temperature and topP
        JSONObject parametersJson = new JSONObject();
        parametersJson.put("temperature", 0.5);
        parametersJson.put("topP", 0.99);
        promptJson.put("generationConfig", parametersJson);

        // Convert the JSON object to a JSON string
        return promptJson.toJSONString();
    }

    public String parseGeminiResponse(String jsonResponse) throws IOException, ParseException {
        // Parse the JSON string
        JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonResponse);

        // Get the "candidates" array
        JSONArray candidatesArray = (JSONArray) jsonObject.get("candidates");

        // Assuming there's only one candidate (index 0), extract its content
        JSONObject candidateObject = (JSONObject) candidatesArray.get(0);
        JSONObject contentObject = (JSONObject) candidateObject.get("content");

        // Get the "parts" array within the content
        JSONArray partsArray = (JSONArray) contentObject.get("parts");

        // Assuming there's only one part (index 0), extract its text
        JSONObject partObject = (JSONObject) partsArray.get(0);
        String responseText = (String) partObject.get("text");

        return responseText;
    }
}
