package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.util.Location;

public class Job {
    private String customerID;
    private String jobID;
    private LocalDateTime bookingTime;
    private LocalDateTime vaTime;
    //private List<Double> startPosition; //old Version Mahkam
    //private List<Double> endPosition; //old Version Mahkam
    private Location startPosition;
    private Location endPosition;


    //####################################################################################
    // Constructors
    //####################################################################################

    public Job(String customerID, String jobID, LocalDateTime bookingTime, LocalDateTime vaTime, Location startPosition, Location endPosition){
        this.customerID = customerID;
        this.jobID = jobID;
        this.bookingTime = bookingTime;
        this.vaTime = vaTime;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }


    public Job(ArrayList<String> values) {

        //String str = "1986-04-08 12:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        //LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

        Double startPosX = Double.parseDouble(values.get(5));
        Double startPosY = Double.parseDouble(values.get(6));
        Double endPosX = Double.parseDouble(values.get(8));
        Double endPosY = Double.parseDouble(values.get(9));

        this.customerID = values.get(0);
        this.jobID = values.get(1);
        this.bookingTime = LocalDateTime.parse(values.get(2), formatter);
        this.vaTime = LocalDateTime.parse(values.get(3), formatter);
        this.startPosition = new Location(values.get(4), startPosX, startPosY);
        this.endPosition = new Location(values.get(7), endPosX, endPosY);
    }

    //####################################################################################
    // method
    //####################################################################################

    public ArrayList<String> toArrayList(){
        ArrayList<String> arrayList = new ArrayList<>(10);
        arrayList.add(customerID);
        arrayList.add(jobID);
        arrayList.add(bookingTime.toString());
        arrayList.add(vaTime.toString());
        arrayList.add(startPosition.getName());
        arrayList.add(Double.toString(startPosition.getX()));
        arrayList.add(Double.toString(startPosition.getY()));
        arrayList.add(endPosition.getName());
        arrayList.add(Double.toString(endPosition.getX()));
        arrayList.add(Double.toString(endPosition.getY()));
        return arrayList;
    };
    public String getCustomerID() {
        return this.customerID;
    }

    public String getID(){
        return jobID;
    }

    public LocalDateTime getbookingTime() {
        return this.bookingTime;
    }

    public LocalDateTime getVATime() {
        return this.vaTime;
    }

    public Location getStartPosition() {
        return this.startPosition;
    }

    public Location getEndPosition() {
        return this.endPosition;
    }

    //  built-in parsers
    public static ArrayList<Job> csvToJobs(String csvPath, char delimiter){
        ArrayList<Job> jobs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm[:ss]");

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line;

            //  skip header line
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(Character.toString(delimiter));

                //  time
                LocalDateTime bookingTime = LocalDateTime.parse(fields[2], formatter);
                LocalDateTime vaTime = LocalDateTime.parse(fields[3], formatter);

                //location
                Location startLocation = new Location("", Double.parseDouble(fields[4]),Double.parseDouble(fields[5]));
                Location endLocation = new Location("", Double.parseDouble(fields[6]),Double.parseDouble(fields[7]));

                //create and add job
                jobs.add(new Job(fields[0], fields[1], bookingTime, vaTime, startLocation, endLocation));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jobs;
    }


    public static List<Job> JSONToJobs(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
                .create();

        // used to capture and preserve the generic type information at runtime
        Type jobsType = new TypeToken<List<Job>>() {}.getType();

        // return json objects
        return gson.fromJson(json, jobsType);
    }

    public static List<Job> JSONFileToJobs(String jsonPath) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
                .create();

        // used to capture and preserve the generic type information at runtime
        Type jobsType = new TypeToken<List<Job>>() {}.getType();

        // return json objects
        return gson.fromJson(JSONParser.readJSONFile(jsonPath), jobsType);
    }
}
