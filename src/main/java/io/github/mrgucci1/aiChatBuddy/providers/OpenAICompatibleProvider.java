package io.github.mrgucci1.aiChatBuddy.providers;

import io.github.mrgucci1.aiChatBuddy.ChatMessage;
import io.github.mrgucci1.aiChatBuddy.ProviderResponse;
import io.github.mrgucci1.aiChatBuddy.ToolCall;
import io.github.mrgucci1.aiChatBuddy.tools.AgentTool;
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
import java.util.logging.Logger;

public class OpenAICompatibleProvider implements AIProvider {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    protected final String baseUrl;
    protected final String apiKey;
    protected final String model;
    protected final double temperature;
    protected final Logger logger;

    public OpenAICompatibleProvider(String baseUrl, String apiKey, String model, double temperature, Logger logger) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.logger = logger;
    }

    @Override
    public ProviderResponse chat(List<ChatMessage> history, List<AgentTool> tools) throws IOException {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", buildMessages(history));
        body.put("temperature", temperature);
        body.put("stream", false);

        if (!tools.isEmpty()) {
            body.put("tools", buildTools(tools));
        }

        String url = baseUrl + "/v1/chat/completions";
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()));

        if (apiKey != null && !apiKey.isEmpty()) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }

        logger.info("[AIProvider] POST " + url + " model=" + model + " messages=" + history.size()
                + " tools=" + tools.size());

        long start = System.currentTimeMillis();
        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
        long elapsed = System.currentTimeMillis() - start;
        logger.info("[AIProvider] Response " + response.statusCode() + " in " + elapsed + "ms ("
                + response.body().length() + " bytes)");

        if (response.statusCode() != 200) {
            throw new IOException("API error " + response.statusCode() + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    @SuppressWarnings("unchecked")
    private JSONArray buildMessages(List<ChatMessage> history) {
        JSONArray messages = new JSONArray();
        for (ChatMessage msg : history) {
            JSONObject m = new JSONObject();
            switch (msg.getRole()) {
                case system:
                    m.put("role", "system");
                    m.put("content", msg.getContent());
                    break;
                case user:
                    m.put("role", "user");
                    m.put("content", msg.getContent());
                    break;
                case assistant:
                    m.put("role", "assistant");
                    if (msg.hasToolCalls()) {
                        JSONArray toolCalls = new JSONArray();
                        for (ToolCall tc : msg.getToolCalls()) {
                            JSONObject tcObj = new JSONObject();
                            tcObj.put("id", tc.getId());
                            tcObj.put("type", "function");
                            JSONObject func = new JSONObject();
                            func.put("name", tc.getName());
                            func.put("arguments", tc.getArgumentsJson());
                            tcObj.put("function", func);
                            toolCalls.add(tcObj);
                        }
                        m.put("tool_calls", toolCalls);
                        // content must be null/absent for tool_call assistant turns
                    } else {
                        m.put("content", msg.getContent() != null ? msg.getContent() : "");
                    }
                    break;
                case tool:
                    m.put("role", "tool");
                    m.put("tool_call_id", msg.getToolCallId());
                    m.put("content", msg.getContent());
                    break;
                default:
                    continue;
            }
            messages.add(m);
        }
        return messages;
    }

    @SuppressWarnings("unchecked")
    private JSONArray buildTools(List<AgentTool> tools) {
        JSONArray result = new JSONArray();
        for (AgentTool tool : tools) {
            JSONObject t = new JSONObject();
            t.put("type", "function");
            JSONObject func = new JSONObject();
            func.put("name", tool.name());
            func.put("description", tool.description());
            func.put("parameters", tool.parametersSchema());
            t.put("function", func);
            result.add(t);
        }
        return result;
    }

    private ProviderResponse parseResponse(String json) throws IOException {
        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(json);
            JSONArray choices = (JSONArray) obj.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IOException("Empty choices in response: " + json);
            }
            JSONObject choice = (JSONObject) choices.get(0);
            JSONObject message = (JSONObject) choice.get("message");

            JSONArray toolCalls = (JSONArray) message.get("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                List<ToolCall> calls = new ArrayList<>();
                for (Object tcObj : toolCalls) {
                    JSONObject tc = (JSONObject) tcObj;
                    String id = (String) tc.get("id");
                    JSONObject func = (JSONObject) tc.get("function");
                    String name = (String) func.get("name");
                    String args = (String) func.get("arguments");
                    calls.add(new ToolCall(id, name, args != null ? args : "{}"));
                }
                return ProviderResponse.toolCalls(calls);
            }

            String content = (String) message.get("content");
            return ProviderResponse.text(content != null ? content : "");
        } catch (ParseException e) {
            throw new IOException("Failed to parse response: " + e.getMessage(), e);
        }
    }
}
