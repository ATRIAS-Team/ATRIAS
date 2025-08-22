package io.github.agentsoz.ees.gui;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.gui.model.*;
import io.github.agentsoz.ees.gui.util.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Map<String, List<Event<?>>> eventsHM = new HashMap<>();

    //2019-12-01
    private static final LocalDate initDate = LocalDate.of(2019, 12, 1);

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
            .registerTypeAdapter(Data.class, new DataDeserializer())
            .setPrettyPrinting()
            .create();

    private static final List<String> tripIds = new ArrayList<>();

    private static final List<LocalDateTime> timeInputs = new ArrayList<>();
    private static final List<Integer> choices = new ArrayList<>();

    private static final List<String> trikesInputs = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        readJSON();
        System.out.println("****************************************************************************************************************************************************");
        System.out.println("EXAMPLE:");
        System.out.println("1. Enter trip id of interest: AP89\n" +
                "1. Enter time of the question sent(HH:mm:ss): 08:02:14\n" +
                "1. QUESTIONS:\n" +
                "\t1) Why is my trike late?\n" +
                "Enter choice: 1\n" +
                "Enter 1 to add more requests: 1\n" +
                "__________________________________________\n" +
                "2. Enter trip id of interest: AP8\n" +
                "2. Enter time of the question sent(HH:mm:ss): 01:32:00\n" +
                "2. QUESTIONS:\n" +
                "\t1) Why is my trike late?\n" +
                "Enter choice: 1\n" +
                "Enter 1 to add more requests: (enter 0 to see results) \n" +
                "__________________________________________\n" +
                "ANSWER 1\n" +
                "Responsible trike id: 2\n" +
                "There is a customerTrip before your trip, that does not finish in time. :false\n" +
                "There is a chargingTrip before your trip, that does not finish in time. :false\n" +
                "__________________________________________\n" +
                "ANSWER 2\n" +
                "Responsible trike id: 3\n" +
                "There is a customerTrip before your trip, that does not finish in time. :true\n" +
                "No predecessor charging trip found\n" +
                "There is a chargingTrip before your trip, that does not finish in time. :false");

        System.out.println("****************************************************************************************************************************************************");
        System.out.println("YOUR INPUT");

        //  INPUT
        Scanner scanner = new Scanner(System.in);
        int i = 0;
        boolean isSuccess;
        do{
            isSuccess = tripIdInput(scanner, i);
            if(!isSuccess){
                break;
            }
            isSuccess = timeInput(scanner, i);
            if (!isSuccess){
                tripIds.remove(i);
                break;
            }
            isSuccess = questionInput(scanner, i);
            if (!isSuccess){
                tripIds.remove(i);
                timeInputs.remove(i);
                break;
            }

            isSuccess = trikesInput(scanner, i);
            if (!isSuccess){
                tripIds.remove(i);
                timeInputs.remove(i);
                choices.remove(i);
                break;
            }

            System.out.print("Enter 1 to add more requests: ");
            String input = scanner.nextLine();
            if(!input.equals("1")){
                break;
            }

            System.out.println("__________________________________________");
            i++;
        }while (true);
        scanner.close();


        for (int j = 0; j < trikesInputs.size(); j++) {
            int startIndex = Integer.MAX_VALUE;
            List<Event<?>> events = eventsHM.get(trikesInputs.get(j));

            for (int k = 0; k < events.size(); k++) {
                Event<?> event = events.get(k);

                if (event.updated.isBefore(timeInputs.get(j)) || event.updated.isEqual(timeInputs.get(j))) {
                    startIndex = k;
                    break;
                }
            }

            switch (choices.get(j)){
                case 1: {
                    System.out.println("__________________________________________");
                    System.out.println("ANSWER " + (j + 1));
                    System.out.println("Responsible trike id: " + trikesInputs.get(j));
                    boolean isCause = isCustomerTripCause(tripIds.get(j), events, startIndex);

                    System.out.println("There is a customerTrip before your trip, that does not finish in time. :" + isCause);

                    boolean isCause2 = isCharginTripCause(tripIds.get(j), events, startIndex);
                    System.out.println("There is a chargingTrip before your trip, that does not finish in time. :" + isCause2);
                    break;
                }
                case 2: {
                    Optional<LocalDateTime> answer = Optional.ofNullable(whenArrive(tripIds.get(j), events, startIndex));
                    if(answer.isPresent()){
                        System.out.println("I will arrive at: " + answer.get());
                    }else{
                        System.out.println("Unknown");
                    }
                    break;
                }
                case 3: {
                    Optional<Location> location = Optional.ofNullable(whereAreYou(tripIds.get(j), events, startIndex));
                    if(location.isPresent()){
                        System.out.println("I'm currently at: " + location.get().x + " " + location.get().y);
                    }else{
                        System.out.println("Unknown");
                    }
                    break;
                }
                case 4: {
                    Optional<LocalDateTime> answer = Optional.ofNullable(whenReach(tripIds.get(j), events, startIndex));
                    if(answer.isPresent()){
                        System.out.println("We will reach the destination at: " + answer.get());
                    }else{
                        System.out.println("Unknown");
                    }
                    break;
                }
                case 5: {
                    Optional<String> answer = Optional.ofNullable(whyResponsible(tripIds.get(j), events, startIndex));
                    if(answer.isPresent()){
                        System.out.println(answer.get());
                    }else{
                        System.out.println("Unknown");
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private static String whyResponsible(String questionerTripID, List<Event<?>> events, int startIndex) {
        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.summary;

            if ("CommitDespiteCNP".equalsIgnoreCase(name)) {
                DecisionTask decisionTask = gson.fromJson(gson.toJson(event.content.data.newValue), DecisionTask.class);
                if(decisionTask.job.jobID.equals(questionerTripID)){
                    return  "The trip was delegated from the taxi control center, " +
                            "got a low utility score, but was still committed after a CNP";
                }
            }else if ("commitNewCustomerRequest".equalsIgnoreCase(name)){
                DecisionTask decisionTask = gson.fromJson(gson.toJson(event.content.data.newValue), DecisionTask.class);
                if(decisionTask.job.jobID.equals(questionerTripID)){
                    return  "The trip was delegated from the taxi control center and got a high utility score";
                }
            }
        }

        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.summary;
            if("TripList_BeliefUpdated".equalsIgnoreCase(name)) {
                try {
                    List<Object> trips = (List<Object>) event.content.data.newValue;


                    for (Object tripObj : trips) {
                        Trip trip = gson.fromJson(gson.toJson(tripObj), Trip.class);
                        if (trip.tripID.equals(questionerTripID)) {
                            return "The trip was delegated by another trike with id " + trip.decisionTask.origin + " after a CNP to this trike";
                        }
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private static LocalDateTime whenArrive(String questionerTripID, List<Event<?>> events, int startIndex){
        //  search for of the trip of the customer
        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.summary;

            if("TripList_BeliefUpdated".equalsIgnoreCase(name)){
                try{
                    List<Object> trips = (List<Object>) event.content.data.newValue;


                    for (Object tripObj: trips){
                        Trip trip = gson.fromJson(gson.toJson(tripObj), Trip.class);
                        if(trip.tripID.equals(questionerTripID)){
                            return trip.arriveTime;
                        }
                    }
                }catch (Exception e){}
            }
        }

        return null;
    }

    private static LocalDateTime whenReach(String questionerTripID, List<Event<?>> events, int startIndex){
        //  search for of the trip of the customer
        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.summary;

            if("TripList_BeliefUpdated".equalsIgnoreCase(name)){
                try{
                    List<Object> trips = (List<Object>) event.content.data.newValue;


                    for (Object tripObj: trips){
                        Trip trip = gson.fromJson(gson.toJson(tripObj), Trip.class);
                        if(trip.tripID.equals(questionerTripID)){
                            return trip.endTime;
                        }
                    }
                }catch (Exception e){}
            }
        }

        return null;
    }

    private static Location whereAreYou(String questionerTripID, List<Event<?>> events, int startIndex){
        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.summary;

            if("AgentPosition_BeliefUpdated".equalsIgnoreCase(name)){
                return gson.fromJson(gson.toJson(event.content.data.newValue), Location.class);
            }
        }
        return null;
    }
    private static boolean tripIdInput(Scanner scanner, int i){
        System.out.print(i + 1 + ". Enter trip id of interest: ");
        String input = scanner.nextLine();

        if (input.matches("AP[0-9]+")) {
            tripIds.add(input);
        }else{
            return false;
        }

        return true;
    }

    public static boolean isCustomerTripCause(String questionerTripID, List<Event<?>> events, int startIndex) {
        boolean causeOfDelay = false;


        int index = -1;

        //  questioner
        LocalDateTime eventTimeOfQuestionerTripCreation = null;
        LocalDateTime questionerTripStartTime = null;


        // predecessor
        String predecessorTripID = null;
        LocalDateTime eventTimeOfPredecessorTripCreation = null;
        LocalDateTime predecessorTripEndTime = null;


        //  search for of the trip of the customer
        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.content.data.name;

            if("CustomerTripCreation".equalsIgnoreCase(name)){
                Map<String, Object> actions = event.content.data.actions;
                Object actionObj = actions.get("Create new CustomerTrip");

                // Gson can't directly cast nested Object to strongly typed object
                String json = gson.toJson(((Map<?, ?>) actionObj).get("decisionTask"));
                DecisionTask decisionTask = gson.fromJson(json, DecisionTask.class);
                String tripID = decisionTask.job.jobID;


                if (tripID.equals(questionerTripID)) {
                    eventTimeOfQuestionerTripCreation = event.updated;
                    questionerTripStartTime = decisionTask.job.bookingTime.withSecond(0);
                    index = i;
                    break;
                }
            }
        }

        if (eventTimeOfQuestionerTripCreation == null) {
            System.err.println("eventTimeOfQuestionerTripCreation is null(question asked too early)");
            return false;
        }

        // search for the predecessor trip
        for (int i = index + 1; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.content.data.name;

            if ("CustomerTripCreation".equalsIgnoreCase(name)) {
                Map<String, Object> actions = event.content.data.actions;
                Object actionObj = actions.get("Create new CustomerTrip");


                String json = gson.toJson(((Map<?, ?>) actionObj).get("decisionTask"));
                DecisionTask decisionTask = gson.fromJson(json, DecisionTask.class);
                String tripID = decisionTask.job.jobID;


                index = i;
                predecessorTripID = tripID;
                eventTimeOfPredecessorTripCreation = event.updated;
                break;
            }
        }

        if (predecessorTripID == null || eventTimeOfPredecessorTripCreation == null) {
            System.out.println("No predecessor customer trip found");
            return false;
        }

        // search for the most recent endtime of the predecessor trip
        for (int i = startIndex; i < index; i++) {
            Event<?> event = events.get(i);

            if ("TripList_BeliefUpdated".equalsIgnoreCase(event.summary)) {
                Data<?> data = event.content.data;

                // Convert newValue to JsonElement, then deserialize to List<Trip>
                JsonElement jsonElement = gson.toJsonTree(data.oldValue);
                Type tripListType = new TypeToken<List<Trip>>() {}.getType();
                List<Trip> trips = gson.fromJson(jsonElement, tripListType);
                boolean contains = false;

                for (Trip trip: trips) {
                    if(trip.tripID.equals(predecessorTripID)){
                        contains = true;
                        predecessorTripEndTime = trip.endTime;
                    }
                }

                if (contains) {
                    if (predecessorTripEndTime.isAfter(questionerTripStartTime)) {
                        causeOfDelay = true;
                    }
                    break;
                }
            }
        }

        return causeOfDelay;
    }

    public static boolean isCharginTripCause(String questionerTripID, List<Event<?>> events, int startIndex) {
        boolean causeOfDelay = false;

        int index = -1;

        //  questioner
        LocalDateTime eventTimeOfQuestionerTripCreation = null;
        LocalDateTime questionerTripStartTime = null;


        // predecessor
        String predecessorTripID = null;
        LocalDateTime eventTimeOfPredecessorTripCreation = null;
        LocalDateTime predecessorTripEndTime = null;


        //  search for of the trip of the customer
        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.content.data.name;

            if("CustomerTripCreation".equalsIgnoreCase(name)){
                Map<String, Object> actions = event.content.data.actions;
                Object actionObj = actions.get("Create new CustomerTrip");

                // Gson can't directly cast nested Object to strongly typed object
                String json = gson.toJson(((Map<?, ?>) actionObj).get("decisionTask"));
                DecisionTask decisionTask = gson.fromJson(json, DecisionTask.class);
                String tripID = decisionTask.job.jobID;


                if (tripID.equals(questionerTripID)) {
                    eventTimeOfQuestionerTripCreation = event.updated;
                    questionerTripStartTime = decisionTask.job.bookingTime.withSecond(0);
                    index = i;
                    break;
                }
            }
        }

        if (eventTimeOfQuestionerTripCreation == null) {
            System.err.println("eventTimeOfQuestionerTripCreation is null(question asked too early)");
            return false;
        }

        // search for the predecessor trip
        for (int i = index + 1; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.content.data.name;

            if ("chargingTripCreation".equalsIgnoreCase(name)) {
                Map<String, Object> actions = event.content.data.actions;
                Object actionObj = actions.get("Create new ChargingTrip");


                // Gson can't directly cast nested Object to strongly typed object
                String tripID = gson.toJson(((Map<?, ?>) actionObj).get("tripID"));;

                index = i;
                predecessorTripID = tripID;
                eventTimeOfPredecessorTripCreation = event.updated;
                break;
            }
        }

        if (predecessorTripID == null || eventTimeOfPredecessorTripCreation == null) {
            System.out.println("No predecessor charging trip found");
            return false;
        }

        // search for the most recent endtime of the predecessor trip
        for (int i = startIndex; i < index; i++) {
            Event<?> event = events.get(i);

            if ("TripList_BeliefUpdated".equalsIgnoreCase(event.summary)) {
                Data<?> data = event.content.data;

                // Convert newValue to JsonElement, then deserialize to List<Trip>
                JsonElement jsonElement = gson.toJsonTree(data.oldValue);
                Type tripListType = new TypeToken<List<Trip>>() {}.getType();
                List<Trip> trips = gson.fromJson(jsonElement, tripListType);
                boolean contains = false;

                for (Trip trip: trips) {
                    if(trip.tripID.equals(predecessorTripID)){
                        contains = true;
                        predecessorTripEndTime = trip.endTime;
                    }
                }

                if (contains) {
                    if (predecessorTripEndTime.isAfter(questionerTripStartTime)) {
                        causeOfDelay = true;
                    }
                    break;
                }
            }
        }

        return causeOfDelay;
    }


    private static boolean timeInput(Scanner scanner, int i){
            System.out.print(i + 1 + ". Enter time of the question sent(HH:mm:ss): ");
            String input = scanner.nextLine();

            // strict time regex
            if (input.matches("([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]")) {
                LocalTime time = LocalTime.parse(input);
                LocalDateTime localDateTime = LocalDateTime.of(initDate, time);
                timeInputs.add(localDateTime);
                return true;
            }else {
                return false;
            }
    }

    private static boolean questionInput(Scanner scanner, int i){
        System.out.println(i + 1 + ". QUESTIONS:");
        System.out.println("\t1) Why is my trike late?");
        System.out.println("\t2) When will you arrive?");
        System.out.println("\t3) What is your position at the moment?");
        System.out.println("\t4) When will I reach my destination?");
        System.out.println("\t5) Why is this trike responsible for me?");
        System.out.print("Enter choice: ");
        try{
            int choice = Integer.parseInt(scanner.nextLine());
            choices.add(choice);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    private static boolean trikesInput(Scanner scanner, int i){
        String trikeId = findMatchTrike(tripIds.get(i));
        if(trikeId == null){
            System.err.println("There is no trikes responsible for trip " + tripIds.get(i));
            return false;
        }else{
            trikesInputs.add(trikeId);
            return true;
        }
    }


    public static void readJSON() throws IOException {
        int counter = 0;
        while (true){
            File file = new File("events/Trike_" + counter + ".json");

            if (!file.exists()) {
                break;
            }

            try (InputStream is = new FileInputStream(file);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                List<Event<?>> parsedEvents = new ArrayList<>();

                for (int i = array.size() - 1; i >= 0; i--) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    String summary = obj.get("summary").getAsString();

                    Event<?> event = gson.fromJson(obj, new TypeToken<Event<?>>() {}.getType());
                    parsedEvents.add(event);
                }
                eventsHM.put(String.valueOf(counter), parsedEvents);
            } catch (IOException e) {
                e.printStackTrace();
            }

            counter ++;
        }
    }

    public static String findMatchTrike(String tripID) {
        Pattern pattern = Pattern.compile("\"" + tripID + "\"");
        for (Map.Entry<String, List<Event<?>>> entry : eventsHM.entrySet()) {
            String trikeId = entry.getKey();
            List<Event<?>> events = entry.getValue();

            for (Event<?> event : events) {
                if ("DecisionTaskCommit".equalsIgnoreCase(event.summary)) {
                    // Serialize oldValue to JSON string
                    String json = gson.toJson(event.content.data.oldValue);
                    Matcher matcher = pattern.matcher(json);
                    if (matcher.find()) {
                        return trikeId;
                    }

                    // Serialize newValue to JSON string (if present)
                    json = gson.toJson(event.content.data.newValue);
                    matcher = pattern.matcher(json);
                    if (matcher.find()) {
                        return trikeId;
                    }
                }
            }
        }
        return null;
    }

}