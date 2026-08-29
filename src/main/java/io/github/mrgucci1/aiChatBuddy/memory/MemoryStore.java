package io.github.mrgucci1.aiChatBuddy.memory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Long-term notes that persist across restarts and reloads, unlike per-player
 * conversation history. Backed by a plain text file, one note per line, shared
 * by every player.
 */
public class MemoryStore {

    private final File file;
    private final int maxEntries;
    private final Logger logger;
    private final List<String> notes = new ArrayList<>();

    public MemoryStore(File file, int maxEntries, Logger logger) {
        this.file = file;
        this.maxEntries = maxEntries;
        this.logger = logger;
        load();
    }

    private synchronized void load() {
        notes.clear();
        if (!file.exists()) return;
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (!line.isBlank()) notes.add(line);
            }
        } catch (IOException e) {
            logger.warning("[Memory] Failed to load " + file.getName() + ": " + e.getMessage());
        }
    }

    public synchronized List<String> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(notes));
    }

    public synchronized void add(String note) {
        String cleaned = note.strip().replace("\n", " ").replace("\r", " ");
        if (cleaned.isEmpty()) return;
        notes.add("[" + LocalDate.now() + "] " + cleaned);
        while (maxEntries > 0 && notes.size() > maxEntries) {
            notes.remove(0);
        }
        persist();
    }

    /** @param oneBasedIndex 1-based index as shown by /aichat memory list */
    public synchronized boolean forget(int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > notes.size()) return false;
        notes.remove(oneBasedIndex - 1);
        persist();
        return true;
    }

    public synchronized void clear() {
        notes.clear();
        persist();
    }

    private void persist() {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.write(file.toPath(), notes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warning("[Memory] Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }

    /** Formats saved notes as a block to append to the system prompt, or "" if there are none. */
    public synchronized String asPromptBlock() {
        if (notes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n\nThings you've saved to memory from past conversations:\n");
        for (String note : notes) {
            sb.append("- ").append(note).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
