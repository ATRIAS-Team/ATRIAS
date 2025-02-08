package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JSONParser {

    public static void csvToJSON(String csvPath, String jsonPath, char delimiter){
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm[:ss]");

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line;

            //  skip header line
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                //  object for 1 job
                jsonObject = new JsonObject();

                String[] fields = line.split(Character.toString(delimiter));

                //  time
                LocalDateTime bookingTime = LocalDateTime.parse(fields[2], formatter);
                LocalDateTime vaTime = LocalDateTime.parse(fields[3], formatter);

                //  start position added -oemer
                JsonObject start = new JsonObject();
                start.addProperty("name", "");
                start.addProperty("x", Double.parseDouble(fields[4]));
                start.addProperty("y", Double.parseDouble(fields[5]));

                //  end position added -oemer
                JsonObject end = new JsonObject();
                end.addProperty("name", "");
                end.addProperty("x", Double.parseDouble(fields[6]));
                end.addProperty("y", Double.parseDouble(fields[7]));


                jsonObject.addProperty("customerID", fields[0]);
                //  setting values to json object
                jsonObject.addProperty("jobID", fields[1]);

                jsonObject.addProperty("bookingTime", bookingTime.toString());
                jsonObject.addProperty("vaTime", vaTime.toString());
                jsonObject.add("startPosition", start);
                jsonObject.add("endPosition", end);
                jsonArray.add(jsonObject);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        writeJSONArray(jsonPath, jsonArray);
    }


    private static void writeJSONArray(String jsonPath, JsonArray jsonArray){
        try (FileWriter writer = new FileWriter(jsonPath)) {
            Gson gson = new Gson();
            gson.toJson(jsonArray, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readJSONFile(String jsonPath){
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
