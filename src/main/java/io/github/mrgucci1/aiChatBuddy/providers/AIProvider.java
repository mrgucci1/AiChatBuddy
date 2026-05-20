package io.github.mrgucci1.aiChatBuddy.providers;

import io.github.mrgucci1.aiChatBuddy.ChatMessage;
import io.github.mrgucci1.aiChatBuddy.ProviderResponse;
import io.github.mrgucci1.aiChatBuddy.tools.AgentTool;

import java.io.IOException;
import java.util.List;

public interface AIProvider {
    ProviderResponse chat(List<ChatMessage> history, List<AgentTool> tools) throws IOException;
}
