package io.github.agentsoz.ees.gui.util;

import com.google.gson.*;
import io.github.agentsoz.ees.gui.model.Data;

import java.lang.reflect.Type;

public class DataDeserializer implements JsonDeserializer<Data<?>> {

    @Override
    public Data<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // Handle "oldValue" and "newValue" together
        JsonElement oldValueElement = jsonObject.get("oldValue");
        JsonElement newValueElement = jsonObject.get("newValue");

        if (oldValueElement != null && newValueElement != null) {
            if (oldValueElement.isJsonObject() || oldValueElement.isJsonArray()) {
                // Both are JSON objects, deserialize as objects
                jsonObject.add("oldValue", oldValueElement);
                jsonObject.add("newValue", newValueElement);
            } else {
                // Both are primitives (numbers, strings, etc.), handle as primitives
                jsonObject.add("oldValue", oldValueElement.getAsJsonPrimitive());
                jsonObject.add("newValue", newValueElement.getAsJsonPrimitive());
            }
        }

        // Now deserialize the whole object
        return new Gson().fromJson(jsonObject, Data.class);
    }
}