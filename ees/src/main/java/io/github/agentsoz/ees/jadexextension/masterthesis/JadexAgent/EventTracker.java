package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared.SharedUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EventTracker<V> {
    public V oldValue;

    private long counter = 0;

    private static final Gson gson = new GsonBuilder()
            .create();

    public void addEvent(Event<V> event, V newValue, String path) throws IOException {
        event.content.eventNumber = this.counter++;
        event.updated = "" + SharedUtils.getSimTime();
        event.content.data.oldValue = oldValue;
        event.content.data.newValue = newValue;
        Type listType = new TypeToken<V>() {}.getType();
        this.oldValue = gson.fromJson(gson.toJson(event.content.data.newValue), listType);
        writeObjectToJsonFile(event, path);
    }
    public static <T> void writeObjectToJsonFile(T object, String path) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(object);

        File file = new File(path);
        if (!file.exists()) {
            Path filePath = Paths.get(path);
            Files.createDirectories(filePath.getParent());

            try (Writer writer = Files.newBufferedWriter(filePath)) {
                writer.write("[" + jsonString + "]");
            }
        } else {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            long length = raf.length();
            if (length > 1) {
                raf.seek(length - 1);
                raf.writeBytes(",\n" + jsonString + "]");
            } else {
                raf.writeBytes("[" + jsonString + "]");
            }
            raf.close();
        }
    }
}
