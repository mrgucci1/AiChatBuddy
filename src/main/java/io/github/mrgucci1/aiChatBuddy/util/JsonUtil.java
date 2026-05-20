package io.github.mrgucci1.aiChatBuddy.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class JsonUtil {

    private JsonUtil() {}

    /** Parses a JSON string into a JSONObject, returning an empty object on null/invalid input. */
    public static JSONObject parseObject(String json) {
        try {
            Object parsed = new JSONParser().parse(json != null ? json : "{}");
            return parsed instanceof JSONObject ? (JSONObject) parsed : new JSONObject();
        } catch (ParseException e) {
            return new JSONObject();
        }
    }
}
