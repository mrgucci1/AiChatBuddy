package io.github.mrgucci1.aiChatBuddy.tools;

import io.github.mrgucci1.aiChatBuddy.memory.MemoryStore;
import org.json.simple.JSONObject;

public class SaveMemoryTool implements AgentTool {

    private final MemoryStore memory;

    public SaveMemoryTool(MemoryStore memory) {
        this.memory = memory;
    }

    @Override
    public String name() { return "save_memory"; }

    @Override
    public String description() {
        return "Permanently save a short note to your memory. Notes persist across restarts and are " +
                "shared by every player. Use this when a player explicitly asks you to remember, save, " +
                "or note something, or right after a web_search/minecraft_wiki result shows your own " +
                "answer was wrong or outdated (e.g. a game update or change you didn't know about) — " +
                "save a brief note about what changed so you know to look it up again next time instead " +
                "of trusting stale knowledge.";
    }

    @Override
    public JSONObject parametersSchema() {
        return AgentTool.singleParamSchema("note",
                "The short fact or reminder to remember, written so it stands on its own later");
    }

    @Override
    public String execute(JSONObject args) {
        String note = (String) args.get("note");
        if (note == null || note.isBlank()) return "error: no note provided";
        memory.add(note);
        return "Saved to memory.";
    }
}
