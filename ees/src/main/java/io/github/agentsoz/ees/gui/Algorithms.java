package io.github.agentsoz.ees.gui;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.gui.model.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.github.agentsoz.ees.gui.util.Utils.gson;

public class Algorithms {
    public static String whyResponsible(String questionerTripID, List<Event<?>> events, int startIndex) {
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
            else if("CommitAsCNPparticipant".equalsIgnoreCase(name)){
                DecisionTask decisionTask = gson.fromJson(gson.toJson(event.content.data.newValue), DecisionTask.class);
                return "The trip was delegated by another trike with id " + decisionTask.origin + " after a CNP to this trike";
            }
        }

        return null;
    }

    public static LocalDateTime whenArrive(String questionerTripID, List<Event<?>> events, int startIndex){
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

    public static LocalDateTime whenReach(String questionerTripID, List<Event<?>> events, int startIndex){
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

    public static Location whereAreYou(String questionerTripID, List<Event<?>> events, int startIndex){
        for (int i = startIndex; i < events.size(); i++) {
            Event<?> event = events.get(i);
            String name = event.summary;

            if("AgentPosition_BeliefUpdated".equalsIgnoreCase(name)){
                return gson.fromJson(gson.toJson(event.content.data.newValue), Location.class);
            }
        }
        return null;
    }

    private static boolean isCustomerTripCause(String questionerTripID, List<Event<?>> events, int startIndex) {
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

    private static boolean isCharginTripCause(String questionerTripID, List<Event<?>> events, int startIndex) {
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

    public static String whyLate(String questionerTripID, List<Event<?>> events, int startIndex){
        boolean isCause = isCustomerTripCause(questionerTripID, events, startIndex);
        boolean isCause2 = isCharginTripCause(questionerTripID, events, startIndex);
        String result = "";
        result += "\nThere is a customerTrip before your trip, that does not finish in time. :" + isCause;
        result += "\nThere is a chargingTrip before your trip, that does not finish in time. :" + isCause2;
        System.out.println(result);
        return result;
    }

}
