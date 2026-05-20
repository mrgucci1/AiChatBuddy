package io.github.mrgucci1.aiChatBuddy.tools;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;

public interface AgentTool {
    String name();
    String description();
    JSONObject parametersSchema();
    String execute(JSONObject args) throws IOException;

    /** Builds a standard single-string "query" parameter schema for tools that take one search term. */
    @SuppressWarnings("unchecked")
    static JSONObject singleQuerySchema(String queryDescription) {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        JSONObject props = new JSONObject();
        JSONObject queryProp = new JSONObject();
        queryProp.put("type", "string");
        queryProp.put("description", queryDescription);
        props.put("query", queryProp);
        schema.put("properties", props);
        JSONArray required = new JSONArray();
        required.add("query");
        schema.put("required", required);
        return schema;
    }
}
