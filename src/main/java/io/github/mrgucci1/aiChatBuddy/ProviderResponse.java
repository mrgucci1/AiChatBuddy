package io.github.mrgucci1.aiChatBuddy;

import java.util.List;

public class ProviderResponse {
    private final String text;
    private final List<ToolCall> toolCalls;

    private ProviderResponse(String text, List<ToolCall> toolCalls) {
        this.text = text;
        this.toolCalls = toolCalls;
    }

    public static ProviderResponse text(String text) {
        return new ProviderResponse(text, null);
    }

    public static ProviderResponse toolCalls(List<ToolCall> toolCalls) {
        return new ProviderResponse(null, toolCalls);
    }

    public boolean isText() { return text != null; }
    public String text() { return text; }
    public List<ToolCall> toolCalls() { return toolCalls; }
}
