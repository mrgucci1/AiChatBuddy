package io.github.mrgucci1.aiChatBuddy;

public class ToolCall {
    private final String id;
    private final String name;
    private final String argumentsJson;

    public ToolCall(String id, String name, String argumentsJson) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getArgumentsJson() { return argumentsJson; }
}
