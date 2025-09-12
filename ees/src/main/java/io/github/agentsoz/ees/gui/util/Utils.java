package io.github.agentsoz.ees.gui.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.gui.model.Data;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Map;

public class Utils {
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
            .registerTypeAdapter(Data.class, new DataDeserializer())
            .setPrettyPrinting()
            .create();

    private static String parseGeminiResponse(String responseJson) {
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();

        // Drill into candidates → 0 → content → parts → 0 → text
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates != null && !candidates.isJsonNull() && candidates.size() > 0) {
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            if (content != null) {
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    JsonObject firstPart = parts.get(0).getAsJsonObject();
                    return firstPart.get("text").getAsString();
                }
            }
        }
        return "No response text found.";
    }

    public static void  promptGemini(String prompt) throws IOException, InterruptedException {
        Gson gson = new Gson();
        // Build the new JSON structure
        JsonObject textObj = new JsonObject();
        textObj.addProperty("text", prompt);

        JsonArray partsArray = new JsonArray();
        partsArray.add(textObj);

        JsonObject contentsObj = new JsonObject();
        contentsObj.add("parts", partsArray);

        JsonArray contentsArray = new JsonArray();
        contentsArray.add(contentsObj);

        JsonObject body = new JsonObject();
        body.add("contents", contentsArray);

        String json = gson.toJson(body);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"))
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", "")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Gemini LLM: " + parseGeminiResponse(response.body()));
    }

    public static void promptLocal(String prompt) throws IOException, InterruptedException {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", "llama3.3:70b");
        jsonObject.addProperty("prompt", prompt);  // Gson handles escaping here
        //jsonObject.addProperty("think", true);
        jsonObject.addProperty("stream", false);

        String json = gson.toJson(jsonObject);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://192.168.1.10:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

        Map<String, Object> responseMap = gson.fromJson(response.body(), mapType);

        Object resp = responseMap.get("response");
        System.out.println("Local LLM: " + resp);
    }
}
