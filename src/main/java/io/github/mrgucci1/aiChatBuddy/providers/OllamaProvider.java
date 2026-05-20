package io.github.mrgucci1.aiChatBuddy.providers;

import java.util.logging.Logger;

public class OllamaProvider extends OpenAICompatibleProvider {

    public OllamaProvider(String baseUrl, String apiKey, String model, double temperature, Logger logger) {
        super(baseUrl, apiKey, model, temperature, logger);
    }
}
