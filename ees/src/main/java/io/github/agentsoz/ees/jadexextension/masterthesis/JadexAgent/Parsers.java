package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Parsers {

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

    public static Element parseXML(String file){
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new FileInputStream(file));
            // Normalisation is optional, but recommended
            // see http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
            // return the root element
            return doc.getDocumentElement();
        } catch(Exception e){
            throw new RuntimeException(e) ;
        }
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
