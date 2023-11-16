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


    public Job(String messageJob) {

        //String str = "1986-04-08 12:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        //LocalDateTime dateTime = LocalDateTime.parse(str, formatter);


        String segments[] = messageJob.split("#");
        String customerID = segments[0];
        String jobID = segments[1];
        //LocalDateTime bookingTime = LocalDateTime.parse(segments[2], formatter); //Error
        //LocalDateTime vaTime = LocalDateTime.parse(segments[3], formatter); //Error
        LocalDateTime bookingTime = LocalDateTime.now(); //Error
        LocalDateTime vaTime = LocalDateTime.now(); //Error


        Double startPosX = Double.parseDouble(segments[4]);
        Double startPosY = Double.parseDouble(segments[5]);
        Double endPosX = Double.parseDouble(segments[6]);
        Double endPosY = Double.parseDouble(segments[7]);

        this.customerID = customerID;
        this.jobID = jobID;
        this.bookingTime = bookingTime;
        this.vaTime = vaTime;
        this.startPosition = new Location("", startPosX, startPosY);
        this.endPosition = new Location("", endPosX, endPosY);

    }

    //####################################################################################
    // method
    //####################################################################################

    public String JobForTransfer(){
        //TODO: find better format for all kind of messages like in Visio extra class

        String messageJob = customerID + "#" + jobID + "#" + bookingTime + "#" + vaTime + "#" + Double.toString(startPosition.getX()) + "#" + Double.toString(startPosition.getY()) + "#" + Double.toString(endPosition.getX()) + "#" + Double.toString(endPosition.getY());

        return messageJob;
    }
    //Serialization and Deserialization






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
