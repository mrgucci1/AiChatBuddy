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
    static JSONObject singleQuerySchema(String queryDescription) {
        return singleParamSchema("query", queryDescription);
    }

    /** Builds a single-string parameter schema with a caller-chosen parameter name. */
    @SuppressWarnings("unchecked")
    static JSONObject singleParamSchema(String paramName, String paramDescription) {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        JSONObject props = new JSONObject();
        JSONObject prop = new JSONObject();
        prop.put("type", "string");
        prop.put("description", paramDescription);
        props.put(paramName, prop);
        schema.put("properties", props);
        JSONArray required = new JSONArray();
        required.add(paramName);
        schema.put("required", required);
        return schema;
    }
}
