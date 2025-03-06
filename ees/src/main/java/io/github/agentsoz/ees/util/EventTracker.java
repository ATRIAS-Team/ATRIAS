package io.github.agentsoz.ees.util;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.shared.SharedUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EventTracker {
    public Object oldValue;

    private long counter = 0;

    private static final Gson gson = new GsonBuilder()
            .create();

    public <V> void addEvent(Event<V> event, V newValue, String path) throws IOException {
        event.content.eventNumber = this.counter++;
        event.updated = "" + SharedUtils.getSimTime();
        event.content.data.oldValue = (V) oldValue;
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
