package io.github.mrgucci1.aiChatBuddy.conversation;

import io.github.mrgucci1.aiChatBuddy.ChatMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManager {

    private final ConcurrentHashMap<UUID, Deque<ChatMessage>> conversations = new ConcurrentHashMap<>();
    private final int maxHistory;

    public ConversationManager(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public List<ChatMessage> get(UUID playerId) {
        Deque<ChatMessage> stored = conversations.get(playerId);
        return stored != null ? new ArrayList<>(stored) : new ArrayList<>();
    }

    public void put(UUID playerId, List<ChatMessage> history) {
        if (maxHistory <= 0) return;
        Deque<ChatMessage> deque = new ArrayDeque<>(history);
        // Keep at most maxHistory*2 messages (user+assistant pairs)
        while (deque.size() > maxHistory * 2) {
            deque.pollFirst();
        }
        conversations.put(playerId, deque);
    }

    public void clear(UUID playerId) {
        conversations.remove(playerId);
    }

    public void clearAll() {
        conversations.clear();
    }

    public int getMaxHistory() {
        return maxHistory;
    }
}
