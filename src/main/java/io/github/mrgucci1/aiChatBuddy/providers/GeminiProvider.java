package io.github.mrgucci1.aiChatBuddy.providers;

import io.github.mrgucci1.aiChatBuddy.ChatMessage;
import io.github.mrgucci1.aiChatBuddy.ProviderResponse;
import io.github.mrgucci1.aiChatBuddy.ToolCall;
import io.github.mrgucci1.aiChatBuddy.tools.AgentTool;
import io.github.mrgucci1.aiChatBuddy.util.JsonUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class GeminiProvider implements AIProvider {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private final String apiKey;
    private final String apiUrl;
    private final double temperature;
    private final double topP;
    private final Logger logger;

    public GeminiProvider(String apiKey, String apiUrl, double temperature, double topP, Logger logger) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.temperature = temperature;
        this.topP = topP;
        this.logger = logger;
    }

    @Override
    public ProviderResponse chat(List<ChatMessage> history, List<AgentTool> tools) throws IOException {
        JSONObject body = new JSONObject();

        // Extract system message and build contents from the rest
        String systemText = null;
        List<ChatMessage> nonSystem = new ArrayList<>();
        for (ChatMessage msg : history) {
            if (msg.getRole() == ChatMessage.Role.system) {
                systemText = msg.getContent();
            } else {
                nonSystem.add(msg);
            }
        }

        if (systemText != null) {
            JSONObject sysInstr = new JSONObject();
            JSONArray sysParts = new JSONArray();
            JSONObject sysPart = new JSONObject();
            sysPart.put("text", systemText);
            sysParts.add(sysPart);
            sysInstr.put("parts", sysParts);
            body.put("system_instruction", sysInstr);
        }

        body.put("contents", buildContents(nonSystem));

        if (!tools.isEmpty()) {
            body.put("tools", buildToolDeclarations(tools));
        }

        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", temperature);
        genConfig.put("topP", topP);
        body.put("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();

        logger.info("[AIProvider] POST gemini messages=" + history.size() + " tools=" + tools.size());

        long start = System.currentTimeMillis();
        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Gemini request interrupted", e);
        }
        long elapsed = System.currentTimeMillis() - start;
        logger.info("[AIProvider] Response " + response.statusCode() + " in " + elapsed + "ms ("
                + response.body().length() + " bytes)");

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API error " + response.statusCode() + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    @SuppressWarnings("unchecked")
    private JSONArray buildContents(List<ChatMessage> history) {
        JSONArray contents = new JSONArray();
        int i = 0;
        while (i < history.size()) {
            ChatMessage msg = history.get(i);
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            if (msg.getRole() == ChatMessage.Role.tool) {
                // Group consecutive tool messages into one user turn
                content.put("role", "user");
                while (i < history.size() && history.get(i).getRole() == ChatMessage.Role.tool) {
                    parts.add(buildFunctionResponsePart(history.get(i)));
                    i++;
                }
            } else if (msg.getRole() == ChatMessage.Role.user) {
                content.put("role", "user");
                JSONObject textPart = new JSONObject();
                textPart.put("text", msg.getContent() != null ? msg.getContent() : "");
                parts.add(textPart);
                i++;
            } else if (msg.getRole() == ChatMessage.Role.assistant) {
                content.put("role", "model");
                if (msg.hasToolCalls()) {
                    for (ToolCall call : msg.getToolCalls()) {
                        parts.add(buildFunctionCallPart(call));
                    }
                } else {
                    JSONObject textPart = new JSONObject();
                    textPart.put("text", msg.getContent() != null ? msg.getContent() : "");
                    parts.add(textPart);
                }
                i++;
            }

            content.put("parts", parts);
            contents.add(content);
        }
        return contents;
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildFunctionCallPart(ToolCall call) {
        JSONObject part = new JSONObject();
        JSONObject fc = new JSONObject();
        fc.put("name", call.getName());
        fc.put("args", JsonUtil.parseObject(call.getArgumentsJson()));
        part.put("functionCall", fc);
        return part;
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildFunctionResponsePart(ChatMessage msg) {
        JSONObject part = new JSONObject();
        JSONObject fr = new JSONObject();
        fr.put("name", msg.getToolName());
        JSONObject frResponse = new JSONObject();
        frResponse.put("output", msg.getContent());
        fr.put("response", frResponse);
        part.put("functionResponse", fr);
        return part;
    }

    @SuppressWarnings("unchecked")
    private JSONArray buildToolDeclarations(List<AgentTool> tools) {
        JSONArray funcDecls = new JSONArray();
        for (AgentTool tool : tools) {
            JSONObject decl = new JSONObject();
            decl.put("name", tool.name());
            decl.put("description", tool.description());
            decl.put("parameters", tool.parametersSchema());
            funcDecls.add(decl);
        }
        JSONObject toolObj = new JSONObject();
        toolObj.put("functionDeclarations", funcDecls);
        JSONArray toolsArray = new JSONArray();
        toolsArray.add(toolObj);
        return toolsArray;
    }

    private ProviderResponse parseResponse(String json) throws IOException {
        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(json);
            JSONArray candidates = (JSONArray) obj.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new IOException("Empty candidates in Gemini response: " + json);
            }
            JSONObject candidate = (JSONObject) candidates.get(0);
            JSONObject content = (JSONObject) candidate.get("content");
            if (content == null) {
                // Blocked or no content — check finishReason
                Object reason = candidate.get("finishReason");
                throw new IOException("Gemini returned no content (finishReason=" + reason + ")");
            }
            JSONArray parts = (JSONArray) content.get("parts");

            StringBuilder sb = new StringBuilder();
            List<ToolCall> calls = new ArrayList<>();
            for (Object partObj : parts) {
                JSONObject part = (JSONObject) partObj;
                // Issue #2 fix: skip thought parts
                Object thought = part.get("thought");
                if (thought instanceof Boolean && (Boolean) thought) continue;

                Object text = part.get("text");
                if (text instanceof String) sb.append((String) text);

                JSONObject fc = (JSONObject) part.get("functionCall");
                if (fc != null) calls.add(toToolCall(fc));
            }

            return calls.isEmpty()
                    ? ProviderResponse.text(sb.toString().trim())
                    : ProviderResponse.toolCalls(calls);
        } catch (ParseException e) {
            throw new IOException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    private ToolCall toToolCall(JSONObject fc) {
        String name = (String) fc.get("name");
        JSONObject args = (JSONObject) fc.get("args");
        String argsJson = args != null ? args.toJSONString() : "{}";
        return new ToolCall(UUID.randomUUID().toString(), name, argsJson);
    }

}
