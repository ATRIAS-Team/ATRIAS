package io.github.agentsoz.ees.gui;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.gui.model.*;
import io.github.agentsoz.ees.gui.util.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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
            System.out.println("##########################################");
            i++;
        }while (true);
        scanner.close();

        readJSON();


        for (int j = 0; j < trikesInputs.size(); j++) {

            switch (choices.get(j)){
                case 1:
                    int startIndex = Integer.MAX_VALUE;
                    List<Event<?>> events = eventsHM.get(trikesInputs.get(j));

                    for (int k = 0; k < events.size(); k++) {
                        Event<?> event = events.get(k);

                        if(event.getUpdated().isBefore(timeInputs.get(j)) || event.getUpdated().isEqual(timeInputs.get(j))){
                            startIndex = k;
                            break;
                        }
                    }
                    boolean isCause = isCustomerTripCause(tripIds.get(j), events, startIndex);
                    System.out.println(j + ") There is a customerTrip before your trip, that does not finish in time. :" + isCause);
                    break;
                case 2:
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean tripIdInput(Scanner scanner, int i){
        System.out.print(i + ". Enter trip id: ");
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
            String name = event.getContent().getData().getName();

            if("CustomerTripCreation".equalsIgnoreCase(name)){
                Map<String, Object> actions = event.getContent().getData().getActions();
                Object actionObj = actions.get("Create new CustomerTrip");

                // Gson can't directly cast nested Object to strongly typed object
                String json = gson.toJson(((Map<?, ?>) actionObj).get("decisionTask"));
                DecisionTask decisionTask = gson.fromJson(json, DecisionTask.class);
                String tripID = decisionTask.getJob().getJobID();


                if (tripID.equals(questionerTripID)) {
                    eventTimeOfQuestionerTripCreation = event.getUpdated();
                    questionerTripStartTime = decisionTask.getJob().getBookingTime();
                    index = i;
                    break;
                }
            }
        }

        if (eventTimeOfQuestionerTripCreation == null) {
            System.out.println("eventTimeOfQuestionerTripCreation is null");
            return false;
        }

        // search for the predecessor trip
        for (int i = index + 1; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.getContent().getData().getName();

            if ("CustomerTripCreation".equalsIgnoreCase(name)) {
                Map<String, Object> actions = event.getContent().getData().getActions();
                Object actionObj = actions.get("Create new CustomerTrip");


                String json = gson.toJson(((Map<?, ?>) actionObj).get("decisionTask"));
                DecisionTask decisionTask = gson.fromJson(json, DecisionTask.class);
                String tripID = decisionTask.getJob().getJobID();


                index = i;
                predecessorTripID = tripID;
                eventTimeOfPredecessorTripCreation = event.getUpdated();
                break;
            }
        }

        if (predecessorTripID == null || eventTimeOfPredecessorTripCreation == null) {
            System.out.println("No predecessor found");
            return false;
        }

        // search for the most recent endtime of the predecessor trip
        for (int i = startIndex; i < index; i++) {
            Event<?> event = events.get(i);

            if ("TripList_BeliefUpdated".equalsIgnoreCase(event.getSummary())) {
                Data<?> data = event.getContent().getData();

                // Convert newValue to JsonElement, then deserialize to List<Trip>
                JsonElement jsonElement = gson.toJsonTree(data.getOldValue());
                Type tripListType = new TypeToken<List<Trip>>() {}.getType();
                List<Trip> trips = gson.fromJson(jsonElement, tripListType);
                boolean contains = false;

                for (Trip trip: trips) {
                    if(trip.getTripID().equals(predecessorTripID)){
                        contains = true;
                        predecessorTripEndTime = trip.getEndTime();
                    }
                }

                if (contains) {
                    predecessorTripEndTime = predecessorTripEndTime.plusHours(1);
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
            System.out.print(i + ". Enter time(HH:mm:ss): ");
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
        System.out.println(i + ". QUESTIONS:");
        System.out.println("1) Why is my trike late?");
        System.out.println("2) Why is this trike responsible for me?");
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
        System.out.print(i + ". Which Trike:");
        String trikeId = scanner.nextLine();
        if(trikeId.matches("[0-9]+")){
            trikesInputs.add(trikeId);
            return true;
        }else {
            return false;
        }
    }


    private static void readJSON() throws IOException {
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
}