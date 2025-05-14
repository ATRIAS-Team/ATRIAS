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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.agentsoz.ees.shared.SharedUtils;
import io.github.agentsoz.ees.trikeagent.DecisionTask;
import io.github.agentsoz.ees.trikeagent.TrikeAgent;
import io.github.agentsoz.ees.trikeagent.TrikeConstants;
import io.github.agentsoz.ees.trikeagent.Trip;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventTracker {
    public Map<String, Object> oldValuesMap = new HashMap<>();
    private long counter = 0;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
            .setPrettyPrinting()
            .create();

    private <V> void addEvent(Event<V> event, V newValue, String path) throws IOException {
        event.content.eventNumber = this.counter;
        event.updated = SharedUtils.getCurrentDateTime();
        if(oldValuesMap.get(event.content.eventType) != null){
            event.content.data.oldValue = (V) oldValuesMap.get(event.content.eventType);
        }
        event.content.data.newValue = newValue;
        Type listType = new TypeToken<V>() {}.getType();
        this.oldValuesMap.put(event.content.eventType, gson.fromJson(gson.toJson(event.content.data.newValue), listType));
        writeObjectToJsonFile(event, path);
        counter++;
    }

    private void addEvent2(Event<XAgProcess> event, String path) throws IOException {
        event.content.eventNumber = this.counter;

        event.updated = SharedUtils.getCurrentDateTime();

        writeObjectToJsonFile(event, path);
        counter++;
    }

    public synchronized void DecisionTaskCommit(TrikeAgent agent, DecisionTask decisionTask){
        try {
            Event<DecisionTask> event = new Event<>();
            event.content.eventType = "DecisionTaskCommit";
            event.content.data = new BeliefData<>();

            event.content.data.trace = "trace";
            event.content.data.belief = "decisionTask";
            event.summary = "DecisionTaskCommit";

            event.content.eventNumber = this.counter;
            event.updated = SharedUtils.getCurrentDateTime();

            event.content.data.oldValue = decisionTask;
            event.content.data.newValue = new DecisionTask(decisionTask, DecisionTask.Status.COMMIT);

            writeObjectToJsonFile(event, "events/Trike_" + agent.agentID + ".json");
            counter++;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public synchronized void TripList_BeliefUpdated(TrikeAgent agent){
        try {
            Event<List<Trip>> event = new Event<>();
            event.content.eventType = "TripList_BeliefUpdated";
            event.content.data = new BeliefData<>();

            event.content.data.trace = "trace";
            event.content.data.belief = "tripList";
            event.summary = "TripList_BeliefUpdated";


            if(oldValuesMap.get(event.content.eventType) == null){
                event.content.data.oldValue = new ArrayList<>();
            }

            this.addEvent(event, agent.tripList,
                    "events/Trike_" + agent.agentID + ".json");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public synchronized void estimateBatteryAfterTIP_BeliefUpdated(TrikeAgent agent, double battery){
        try {
            Event<Double> event = new Event<>();
            event.content.eventType = "estimateBatteryAfterTIP_BeliefUpdated";
            event.content.data = new BeliefData<>();

            event.content.data.trace = "trace";
            event.content.data.belief = "estimateBatteryAfterTIP";
            event.summary = "estimateBatteryAfterTIP_BeliefUpdated";

            if(oldValuesMap.get(event.content.eventType) == null){
                event.content.data.oldValue = 0.9;
            }


            this.addEvent(event, battery,
                    "events/Trike_" + agent.agentID + ".json");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private synchronized void addXAgProcess(TrikeAgent agent,  XAgProcess xAgProcess){
        try {
            Event<XAgProcess> event = new Event<>();
            event.content.eventType = "XAgentProcess";

            event.content.data = xAgProcess;

            event.summary = "XAgentProcess";

            this.addEvent2(event,
                    "events/Trike_" + agent.agentID + ".json");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public synchronized void CustomerTripCreation(TrikeAgent agent, Trip trip){
        Map<String, Object> queries = new HashMap<>();
        //  hier queries
        queries.put("DecisionTaskList", agent.decisionTasks.values());

        Map<String, Object> actions = new HashMap<>();
        actions.put("DecisionTask " + trip.decisionTask.getJobID() + " status", "committed");
        actions.put("Create new CustomerTrip", trip);

        XAgProcess xAgProcess = XAgProcess.builder()
                .name("customerTripCreation")
                .trigger("DecisionTaskCommit")
                .queries(queries)
                .criterion("A DecisionTask which status equals commit will caue the creation of a customerTrip")
                .actions(actions)
                .notification("DecisionTaskCommited", "TripList_BeliefUpdated")
                .build();

        addXAgProcess(agent, xAgProcess);
    }

    public synchronized void ChargingTripCreation(TrikeAgent agent, Trip trip){
        Map<String, Object> queries = new HashMap<>();

        queries.put("1", "estimateBatteryAfterTIP_BeliefUpdated");
        queries.put("CHARGING_THRESHHOLD", TrikeConstants.CHARGING_THRESHOLD);
        queries.put("3", "chargingTripAvailable");


        Map<String, Object> actions = new HashMap<>();
        actions.put("chargingTripAvailable", "chargingTripAvailable + 1");
        actions.put("Create new ChargingTrip", trip);

        XAgProcess xAgProcess = XAgProcess.builder()
                .name("chargingTripCreation")
                .trigger("estimateBatteryAfterTIP_BeliefUpdated")
                .queries(queries)
                .criterion("IF((estimateBatteryAfterTIP < CHARGING_THRESHOLD) & (chargingTripAvailable == 0))")
                .actions(actions)
                .notification("TripList_BeliefUpdated", "chargingTripAvailable_BeliefUpdated")
                .build();

        addXAgProcess(agent, xAgProcess);
    }


    public static <T> void writeObjectToJsonFile(T object, String path) throws IOException {
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
