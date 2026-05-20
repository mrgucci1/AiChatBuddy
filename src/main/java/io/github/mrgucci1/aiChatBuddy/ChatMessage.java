package io.github.mrgucci1.aiChatBuddy;

import java.util.List;

public class ChatMessage {
    public enum Role { user, assistant, tool, system }

    private final Role role;
    private final String content;
    private final List<ToolCall> toolCalls;
    private final String toolCallId;
    private final String toolName;

    private ChatMessage(Role role, String content, List<ToolCall> toolCalls, String toolCallId, String toolName) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.user, content, null, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.assistant, content, null, null, null);
    }

    public static ChatMessage assistantToolCalls(List<ToolCall> toolCalls) {
        return new ChatMessage(Role.assistant, null, toolCalls, null, null);
    }

    public static ChatMessage tool(String toolCallId, String toolName, String content) {
        return new ChatMessage(Role.tool, content, null, toolCallId, toolName);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.system, content, null, null, null);
    }

    public Role getRole() { return role; }
    public String getContent() { return content; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public String getToolCallId() { return toolCallId; }
    public String getToolName() { return toolName; }
    public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
}
