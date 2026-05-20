package io.github.mrgucci1.aiChatBuddy.agent;

import io.github.mrgucci1.aiChatBuddy.ChatMessage;
import io.github.mrgucci1.aiChatBuddy.ProviderResponse;
import io.github.mrgucci1.aiChatBuddy.ToolCall;
import io.github.mrgucci1.aiChatBuddy.conversation.ConversationManager;
import io.github.mrgucci1.aiChatBuddy.providers.AIProvider;
import io.github.mrgucci1.aiChatBuddy.tools.AgentTool;
import io.github.mrgucci1.aiChatBuddy.util.JsonUtil;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class AgentLoop {

    private final AIProvider provider;
    private final ConversationManager convo;
    private final List<AgentTool> tools;
    private final int maxSteps;
    private final Logger logger;
    private final Consumer<String> broadcast;

    /**
     * @param broadcast called between tool-calling steps; receives a formatted chat string
     */
    public AgentLoop(AIProvider provider, ConversationManager convo, List<AgentTool> tools,
                     int maxSteps, Logger logger, Consumer<String> broadcast) {
        this.provider = provider;
        this.convo = convo;
        this.tools = tools;
        this.maxSteps = maxSteps;
        this.logger = logger;
        this.broadcast = broadcast;
    }

    public String ask(UUID playerId, String question, String systemPrompt) {
        List<ChatMessage> fullHistory = new ArrayList<>();

        // Prepend system prompt; track where storable messages begin
        final int storeOffset;
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            fullHistory.add(ChatMessage.system(systemPrompt));
            storeOffset = 1;
        } else {
            storeOffset = 0;
        }

        fullHistory.addAll(convo.get(playerId));
        fullHistory.add(ChatMessage.user(question));

        logger.info("[AgentLoop] Starting (player=" + playerId + ", tools=" + tools.size()
                + ", maxSteps=" + maxSteps + ")");

        for (int step = 0; step < maxSteps; step++) {
            logger.info("[AgentLoop] Step " + (step + 1) + "/" + maxSteps);
            ProviderResponse r;
            try {
                r = provider.chat(fullHistory, tools);
            } catch (IOException e) {
                logger.warning("[AgentLoop] Provider error: " + e.getMessage());
                return "An error occurred while contacting the AI provider.";
            }

            if (r.isText()) {
                fullHistory.add(ChatMessage.assistant(r.text()));
                persist(playerId, fullHistory, storeOffset);
                logger.info("[AgentLoop] Finished with text reply (" + r.text().length() + " chars)");
                return r.text();
            }

            fullHistory.add(ChatMessage.assistantToolCalls(r.toolCalls()));
            for (ToolCall call : r.toolCalls()) {
                logger.info("[AgentLoop] Invoking tool: " + call.getName());
                if (broadcast != null) {
                    broadcast.accept("§7[Searching: " + call.getName() + "...]");
                }
                fullHistory.add(ChatMessage.tool(call.getId(), call.getName(), runTool(call)));
            }
        }

        logger.info("[AgentLoop] Max steps reached — forcing best-effort synthesis without tools");
        fullHistory.add(ChatMessage.user(
                "You've used all your tool calls. Answer the original question as best you can " +
                "with the information you've already gathered. Do not request more tools."));
        try {
            ProviderResponse synth = provider.chat(fullHistory, Collections.emptyList());
            if (synth.isText() && synth.text() != null && !synth.text().isBlank()) {
                fullHistory.add(ChatMessage.assistant(synth.text()));
                persist(playerId, fullHistory, storeOffset);
                logger.info("[AgentLoop] Best-effort reply (" + synth.text().length() + " chars)");
                return synth.text();
            }
        } catch (IOException e) {
            logger.warning("[AgentLoop] Best-effort synthesis failed: " + e.getMessage());
        }
        return "I hit my tool-use limit before finding a good answer.";
    }

    /**
     * Persist only user + plain-assistant turns. Intermediate tool_call / tool_response
     * messages are dropped so history can never be trimmed into an orphan call/response
     * pair (Gemini rejects this with 400 INVALID_ARGUMENT).
     */
    private void persist(UUID playerId, List<ChatMessage> fullHistory, int storeOffset) {
        if (convo.getMaxHistory() <= 0) return;
        List<ChatMessage> clean = new ArrayList<>();
        for (int i = storeOffset; i < fullHistory.size(); i++) {
            ChatMessage m = fullHistory.get(i);
            if (m.getRole() == ChatMessage.Role.user) {
                clean.add(m);
            } else if (m.getRole() == ChatMessage.Role.assistant
                    && !m.hasToolCalls()
                    && m.getContent() != null && !m.getContent().isBlank()) {
                clean.add(m);
            }
        }
        convo.put(playerId, clean);
    }

    private String runTool(ToolCall call) {
        for (AgentTool tool : tools) {
            if (tool.name().equals(call.getName())) {
                try {
                    JSONObject args = JsonUtil.parseObject(call.getArgumentsJson());
                    return tool.execute(args);
                } catch (Exception e) {
                    logger.log(java.util.logging.Level.WARNING,
                            "[AgentLoop] Tool " + call.getName() + " failed: " + e.getMessage(), e);
                    return "error: " + e.getMessage();
                }
            }
        }
        logger.warning("[AgentLoop] Unknown tool requested: " + call.getName());
        return "error: unknown tool " + call.getName();
    }
}
