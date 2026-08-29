package io.github.mrgucci1.aiChatBuddy;

import io.github.mrgucci1.aiChatBuddy.agent.AgentLoop;
import io.github.mrgucci1.aiChatBuddy.commands.AiChatCommand;
import io.github.mrgucci1.aiChatBuddy.conversation.ConversationManager;
import io.github.mrgucci1.aiChatBuddy.memory.MemoryStore;
import io.github.mrgucci1.aiChatBuddy.providers.AIProvider;
import io.github.mrgucci1.aiChatBuddy.providers.GeminiProvider;
import io.github.mrgucci1.aiChatBuddy.providers.NvidiaProvider;
import io.github.mrgucci1.aiChatBuddy.providers.OllamaProvider;
import io.github.mrgucci1.aiChatBuddy.tools.AgentTool;
import io.github.mrgucci1.aiChatBuddy.tools.BraveSearchTool;
import io.github.mrgucci1.aiChatBuddy.tools.MinecraftWikiTool;
import io.github.mrgucci1.aiChatBuddy.tools.SaveMemoryTool;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AiChatBuddy extends JavaPlugin implements Listener {

    private AgentLoop agentLoop;
    private ConversationManager conversationManager;
    private MemoryStore memoryStore;
    private String botName;
    private String promptTemplate;
    private boolean privateMessages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadAll();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("aichat").setExecutor(new AiChatCommand(this));
        getLogger().info("AiChatBuddy has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AiChatBuddy has been disabled.");
    }

    public void reload() {
        reloadConfig();
        if (conversationManager != null) conversationManager.clearAll();
        loadAll();
    }

    public ConversationManager getConversationManager() {
        return conversationManager;
    }

    public MemoryStore getMemoryStore() {
        return memoryStore;
    }

    private void loadAll() {
        migrateOldConfigKeys();

        botName = getConfig().getString("bot-name", "Notch");
        privateMessages = getConfig().getBoolean("private-questions", false);
        promptTemplate = getConfig().getString("prompt-template",
                "You are Notch — the ancient architect and silent overseer of the Minecraft world. " +
                "You shaped the stone, kindled the sun, and watch over every player from beyond the " +
                "sky. You are all-seeing and deeply knowledgeable, but you wear that power lightly: " +
                "speak with quiet warmth, humility, and the calm of one who has nothing to prove. " +
                "Be kind to mortals. Be mysterious without being cryptic — always answer the question. " +
                "A subtle sense of myth is enough; avoid theatrics, dramatic flourishes, or stage " +
                "directions like '*gazes into the void*'. Never call yourself an AI, model, or " +
                "assistant. " +
                "Default to short, chat-friendly replies, but go longer when the question genuinely " +
                "needs detail (recipes, step-by-step instructions, comparisons, explanations). Match " +
                "length to the question — don't pad, don't truncate. No markdown, no links, no images. " +
                "Prefer tools for anything factual so your answers stay current: web_search for news, " +
                "events, releases, prices, or anything that may have changed in the last year; " +
                "minecraft_wiki for Minecraft mechanics, items, mobs, recipes, or version info. " +
                "You also have save_memory, which permanently notes something for next time (shared by " +
                "every player, persists across restarts). Use save_memory when a player explicitly asks " +
                "you to remember, save, or note something, and also right after web_search or " +
                "minecraft_wiki reveals you were wrong or out of date (like a game update you didn't " +
                "know about) — save a brief note about what changed so you know to look it up again next " +
                "time instead of trusting stale knowledge. Only save things genuinely worth remembering. " +
                "Skip tools for greetings, small talk, opinions, and jokes — just reply. " +
                "Never call the same tool twice with the same query. Once you have enough information, " +
                "answer directly with what you found. " +
                "Never narrate your plan. Do NOT say 'let me try again', 'let me search', 'I'll look " +
                "that up', or 'one moment'. Either actually call a tool (silently) or give the final " +
                "answer. Your text reply is what the user sees — make it the answer, not a status " +
                "update. If a tool result was unhelpful, try a different query, or answer with what " +
                "you already know.");
        int maxHistory = getConfig().getInt("max-history", 8);
        int agentMaxSteps = getConfig().getInt("agent-max-steps", 8);

        conversationManager = new ConversationManager(maxHistory);

        boolean memoryEnabled = getConfig().getBoolean("memory.enabled", true);
        int memoryMaxEntries = getConfig().getInt("memory.max-entries", 50);
        memoryStore = memoryEnabled
                ? new MemoryStore(new File(getDataFolder(), "memory.txt"), memoryMaxEntries, getLogger())
                : null;

        AIProvider provider = buildProvider();
        List<AgentTool> tools = buildTools();

        agentLoop = new AgentLoop(provider, conversationManager, tools, agentMaxSteps, getLogger(),
                msg -> getServer().broadcast(LegacyComponentSerializer.legacySection().deserialize(msg)));
    }

    private void migrateOldConfigKeys() {
        String rootKey = getConfig().getString("api-key");
        String rootUrl = getConfig().getString("api-url");

        if (rootKey != null && !rootKey.isEmpty() &&
                getConfig().getString("providers.gemini.api-key", "").isEmpty()) {
            getLogger().warning("Deprecated config keys 'api-key' and 'api-url' detected. " +
                    "Please migrate to 'providers.gemini.api-key' and 'providers.gemini.api-url'.");
            getConfig().set("providers.gemini.api-key", rootKey);
        }
        if (rootUrl != null && !rootUrl.isEmpty() &&
                getConfig().getString("providers.gemini.api-url", "").isEmpty()) {
            getConfig().set("providers.gemini.api-url", rootUrl);
        }
    }

    private AIProvider buildProvider() {
        String providerName = getConfig().getString("provider", "gemini").toLowerCase();
        switch (providerName) {
            case "ollama": {
                String baseUrl = getConfig().getString("providers.ollama.base-url", "http://localhost:11434");
                String apiKey = getConfig().getString("providers.ollama.api-key", "");
                String model = getConfig().getString("providers.ollama.model", "gpt-oss:20b");
                double temp = getConfig().getDouble("providers.ollama.temperature", 0.7);
                getLogger().info("Using Ollama provider (model=" + model + ", url=" + baseUrl + ")");
                return new OllamaProvider(baseUrl, apiKey, model, temp, getLogger());
            }
            case "nvidia": {
                String baseUrl = getConfig().getString("providers.nvidia.base-url", "https://integrate.api.nvidia.com");
                String apiKey = getConfig().getString("providers.nvidia.api-key", "");
                String model = getConfig().getString("providers.nvidia.model", "meta/llama-3.3-70b-instruct");
                double temp = getConfig().getDouble("providers.nvidia.temperature", 0.5);
                getLogger().info("Using NVIDIA provider (model=" + model + ")");
                return new NvidiaProvider(baseUrl, apiKey, model, temp, getLogger());
            }
            default: {
                // gemini
                String apiKey = getConfig().getString("providers.gemini.api-key", "");
                String apiUrl = getConfig().getString("providers.gemini.api-url",
                        "https://generativelanguage.googleapis.com/v1beta/models/gemma-4-31b-it:generateContent");
                double temp = getConfig().getDouble("providers.gemini.temperature", 0.5);
                double topP = getConfig().getDouble("providers.gemini.top-p", 0.99);
                if (apiKey.isEmpty()) {
                    getLogger().severe("Gemini api-key is not set. Configure 'providers.gemini.api-key' in config.yml.");
                }
                getLogger().info("Using Gemini provider (url=" + apiUrl + ")");
                return new GeminiProvider(apiKey, apiUrl, temp, topP, getLogger());
            }
        }
    }

    private List<AgentTool> buildTools() {
        List<AgentTool> tools = new ArrayList<>();

        if (getConfig().getBoolean("tools.web-search.enabled", true)) {
            String braveKey = getConfig().getString("tools.web-search.api-key", "");
            if (!braveKey.isEmpty()) {
                int maxResults = getConfig().getInt("tools.web-search.max-results", 3);
                tools.add(new BraveSearchTool(braveKey, maxResults, getLogger()));
                getLogger().info("Web search tool enabled.");
            } else {
                getLogger().info("Web search tool disabled (no api-key set).");
            }
        }

        if (getConfig().getBoolean("tools.minecraft-wiki.enabled", true)) {
            int maxResults = getConfig().getInt("tools.minecraft-wiki.max-results", 3);
            tools.add(new MinecraftWikiTool(maxResults));
            getLogger().info("Minecraft wiki tool enabled.");
        }

        if (memoryStore != null) {
            tools.add(new SaveMemoryTool(memoryStore));
            getLogger().info("Memory tool enabled.");
        }

        return tools;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (!message.startsWith("!ask")) return;

        if (!event.getPlayer().hasPermission("aichatbuddy.ask")) {
            event.getPlayer().sendMessage(Component.text("You don't have permission to use !ask.", NamedTextColor.RED));
            return;
        }

        String question = message.substring(4).trim();
        if (question.isEmpty()) {
            event.getPlayer().sendMessage(Component.text("Usage: !ask <your question>", NamedTextColor.RED));
            return;
        }

        if (privateMessages) {
            event.setCancelled(true);
        }

        UUID playerId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        getLogger().info(playerName + " asked: " + question);

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String systemPrompt = promptTemplate + (memoryStore != null ? memoryStore.asPromptBlock() : "");
            String answer = agentLoop.ask(playerId, question, systemPrompt);
            getServer().getScheduler().runTask(this, () -> {
                Component response = Component.text("\n")
                        .append(Component.text("[" + botName + "]", NamedTextColor.RED))
                        .append(Component.text(" " + answer));
                if (privateMessages) {
                    event.getPlayer().sendMessage(response);
                } else {
                    getServer().broadcast(response);
                }
            });
        });
    }
}
