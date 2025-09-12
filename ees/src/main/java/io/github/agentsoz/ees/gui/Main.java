package io.github.agentsoz.ees.gui;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.gui.model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.agentsoz.ees.gui.Algorithms.*;
import static io.github.agentsoz.ees.gui.util.Utils.*;

public class Main {

    private static final Map<String, List<Event<?>>> eventsHM = new HashMap<>();

    //2019-12-01
    private static final LocalDate initDate = LocalDate.of(2019, 12, 1);

    private static final List<String> tripIds = new ArrayList<>();

    private static final List<LocalDateTime> timeInputs = new ArrayList<>();
    private static final List<Integer> choices = new ArrayList<>();

    private static final List<String> trikesInputs = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {
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

            if (addMatchTrike(tripIds.get(i)) == null){
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

        answers();
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

    /**
     * Here you can add more questions
     * */
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

    /**
     * Here you add the answer
     * */
    private static void answers() throws IOException, InterruptedException {
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


                    Path path = Paths.get("prompts/late-prof.txt");

                    String prompt = whyLate(tripIds.get(j), events, startIndex);

                    runPrompt(prompt, "prompts/late-prof.txt");
                    break;
                }
                case 2: {
                    String prompt = "";

                    Optional<LocalDateTime> answer = Optional.ofNullable(whenArrive(tripIds.get(j), events, startIndex));
                    if(answer.isPresent()){
                        System.out.println("I will arrive at: " + answer.get());
                        prompt += "I will arrive at: " + answer.get();
                    }else{
                        System.out.println("Unknown");
                        prompt+= "Unknown";
                    }
                    runPrompt(prompt, "prompts/arrive-prof.txt");
                    break;
                }
                case 3: {
                    String prompt = "";

                    Optional<Location> location = Optional.ofNullable(whereAreYou(tripIds.get(j), events, startIndex));
                    if(location.isPresent()){
                        System.out.println("I'm currently at: " + location.get().x + " " + location.get().y);
                        prompt += "I'm currently at: " + location.get().x + " " + location.get().y;
                    }else{
                        System.out.println("Unknown");
                        prompt += "Unknown";
                    }
                    runPrompt(prompt, "prompts/where-prof.txt");
                    break;
                }
                case 4: {
                    String prompt = "";
                    Optional<LocalDateTime> answer = Optional.ofNullable(whenReach(tripIds.get(j), events, startIndex));
                    if(answer.isPresent()){
                        System.out.println("We will reach the destination at: " + answer.get());
                        prompt += "We will reach the destination at: " + answer.get();
                    }else{
                        System.out.println("Unknown");
                        prompt += "Unknown";
                    }
                    runPrompt(prompt, "prompts/reach-genz.txt");
                    break;
                }
                case 5: {
                    String prompt = "";
                    Optional<String> answer = Optional.ofNullable(whyResponsible(tripIds.get(j), events, startIndex));
                    if(answer.isPresent()){
                        System.out.println(answer.get());
                        prompt += answer.get();
                    }else{
                        System.out.println("Unknown");
                        prompt += "Unknown";
                    }
                    runPrompt(prompt, "prompts/why-prof.txt");
                    break;
                }
                default:
                    break;
            }
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

    /**
     * Finds the TrikeAgent that has committed the trip.
     * */
    public static String addMatchTrike(String tripID) {
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
                        trikesInputs.add(trikeId);
                        return trikeId;
                    }

                    // Serialize newValue to JSON string (if present)
                    json = gson.toJson(event.content.data.newValue);
                    matcher = pattern.matcher(json);
                    if (matcher.find()) {
                        trikesInputs.add(trikeId);
                        return trikeId;
                    }
                }
            }
        }

        System.err.println("There is no trikes responsible for trip " + tripID);
        return null;
    }


    /**
     * Here you can decide which LLMs to prompt.
     * */
    public static void runPrompt(String extraPrompt, String pathString) throws IOException, InterruptedException {
        Path path = Paths.get(pathString);
        String prompt = Files.readString(path);

        // Append extra instructions or variables
        prompt += "\n" + extraPrompt;

       promptGemini(prompt);
       promptLocal(prompt);
    }
}