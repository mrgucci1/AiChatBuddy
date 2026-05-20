package io.github.mrgucci1.aiChatBuddy.providers;

import java.util.logging.Logger;

public class NvidiaProvider extends OpenAICompatibleProvider {

    public NvidiaProvider(String baseUrl, String apiKey, String model, double temperature, Logger logger) {
        super(baseUrl, apiKey, model, temperature, logger);
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warning("NVIDIA provider requires an API key (nvapi-...).");
        } else if (!apiKey.startsWith("nvapi-")) {
            logger.warning("NVIDIA API key should start with 'nvapi-'. Check your config.");
        }
    }
}
