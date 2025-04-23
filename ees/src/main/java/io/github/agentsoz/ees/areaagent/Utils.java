package io.github.agentsoz.ees.areaagent;

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

import io.github.agentsoz.ees.Run.XMLConfig;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.shared.*;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.Run.JadexModel;
import io.github.agentsoz.ees.Run.TrikeMain;
import io.github.agentsoz.util.Location;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import org.w3c.dom.Element;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    AreaAgent areaAgent;

    public Utils(AreaAgent agent){
        this.areaAgent = agent;
    }

    public void body(){
        Element classElement = XMLConfig.getClassElement("AreaAgent.java");
        AreaConstants.configure(classElement);
        Pattern pattern = Pattern.compile("[0-9]+");
        Matcher matcher = pattern.matcher(areaAgent.agent.getId().getLocalName());
        int index = 0;

        if (matcher.find()) {
            index = Integer.parseInt(matcher.group());
        }

        areaAgent.locatedAgentList.setAreaAgent(areaAgent);

        areaAgent.areaAgentId = "area: " + index;
        areaAgent.myTag = areaAgent.areaAgentId;
        areaAgent.cell = Cells.areaAgentCells.get(index);
        Cells.cellAgentMap.put(areaAgent.cell, areaAgent.areaAgentId);
        areaAgent.neighbourIds = Cells.getNeighbours(areaAgent.cell);

        System.out.println("AreaAgent " + areaAgent.areaAgentId + " sucessfully started;");
        initJobs();

        IServiceIdentifier sid = ((IService) areaAgent.agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
        areaAgent.agent.setTags(sid, areaAgent.areaAgentId);
        System.out.println("locatedAgentList size: " + areaAgent.locatedAgentList.size());

        if(SharedConstants.FIREBASE_ENABLED) {
            //  fetch jobs from firebase
            FirebaseHandler<AreaAgent, Job> firebaseHandler = new FirebaseHandler<AreaAgent, Job>(areaAgent, areaAgent.jobList);
            firebaseHandler.childAddedListener("tripRequests", (dataSnapshot, previousChildName, list) -> {
                // A new child node has been added
                String tripRequestId = dataSnapshot.getKey();
                System.out.println("New trip request added: " + tripRequestId);

                // Access data of the new trip request
                //String assignedAgent = dataSnapshot.child("assignedAgent").getValue(String.class);
                String customerId = dataSnapshot.child("customerId").getValue(String.class);
                String startTimeStr = dataSnapshot.child("startTime").getValue(String.class);
                String timestampStr = dataSnapshot.child("timestamp").getValue(String.class);

                //System.out.println("Assigned Agent: " + assignedAgent);
                System.out.println("Customer ID: " + customerId);
                System.out.println("Start Time: " + startTimeStr);
                System.out.println("Timestamp: " + timestampStr);


                Location startPosition = new Location("", 0, 0);
                startPosition.x = dataSnapshot.child("startLocation").child("longitude").getValue(Double.class);
                startPosition.y = dataSnapshot.child("startLocation").child("latitude").getValue(Double.class);

                Location endPosition = new Location("", 0, 0);
                endPosition.x = dataSnapshot.child("endLocation").child("longitude").getValue(Double.class);
                endPosition.y = dataSnapshot.child("endLocation").child("latitude").getValue(Double.class);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS'Z'");
                System.out.println(startTimeStr);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
                LocalTime localTime = LocalTime.parse(startTimeStr, dtf);
                LocalDateTime startTime = LocalDateTime.of(LocalDate.now(), localTime);
                LocalDateTime vaTime = LocalDateTime.parse(timestampStr, formatter);
                Job job = new Job(customerId, tripRequestId, startTime, vaTime, startPosition, endPosition);

                String jobCell = Cells.locationToCellAddress(job.getStartPosition(), Cells.getCellResolution(areaAgent.cell));
                if (jobCell.equals(areaAgent.cell)) {
                    list.add(job);
                }
            });
        }

        while (TrikeMain.TrikeAgentNumber != JadexModel.TrikeAgentnumber) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void sendJobToAgent(List<Job> jobList){
        while (true){
            //  current job
            Job job = jobList.get(0);
            if(job == null) break;
            long jobTimeStamp = SharedUtils.getTimeStamp(job.getVATime());
            long simTimeStamp = SharedUtils.getSimTime();
            if(jobTimeStamp > simTimeStamp) break;

            if(Objects.equals(Cells.findKey(job.getStartPosition()), areaAgent.cell)){
                if(areaAgent.load >= AreaConstants.NO_TRIKES_NO_TRIPS_LOAD){
                    areaAgent.load += 1.0;
                }else{
                    areaAgent.load += 1.0 / areaAgent.locatedAgentList.size();
                }
            }

            String closestAgent = areaAgent.locatedAgentList.calculateClosestLocatedAgent(job.getStartPosition());
            if (closestAgent == null){
                if(!job.getID().startsWith("area")){
                    areaAgent.jobsToDelegate.add(new DelegateInfo(job));
                    System.out.println(job.getID() + " is delegated");
                }
                jobList.remove(0);
            }
            else{
                //message creation
                MessageContent messageContent = new MessageContent("", job.toArrayList());
                LocalTime bookingTime = LocalTime.now();
                System.out.println("START Negotiation - JobID: " + job.getID() + " Time: "+ bookingTime);
                Message message = new Message(areaAgent.areaAgentId, closestAgent, Message.ComAct.REQUEST, SharedUtils.getSimTime(), messageContent);
                areaAgent.requests.add(message);
                SharedUtils.sendMessage(message.getReceiverId(), message.serialize());
                //remove job from list
                jobList.remove(0);
                System.out.println("AREA AGENT: JOB was SENT");
            }
        }
    }


    private void initJobs() {
        String csvFilePath = AreaConstants.CSV_SOURCE;
        char delimiter = ';';
        int startCounter = 0;
        int endCounter = 0;
        Set<String> set = new HashSet<>();

        System.out.println("parse json from file:");
        List<Job> allJobs = Job.csvToJobs(csvFilePath, delimiter);
        for (Job job : allJobs) {
            String jobCell = Cells.locationToCellAddress(job.getStartPosition(), Cells.getCellResolution(areaAgent.cell));
            if(jobCell.equals(areaAgent.cell)){
                areaAgent.csvJobList.add(job);
                startCounter++;
            }
            String jobEndCell = Cells.locationToCellAddress(job.getEndPosition(), Cells.getCellResolution(areaAgent.cell));
            if(jobEndCell.equals(areaAgent.cell)){
                endCounter++;
            }
            set.add(jobCell);
        }
        System.out.println(set);
        System.out.println(areaAgent.areaAgentId + ": " + startCounter + " " + endCounter);
        for (Job job: areaAgent.csvJobList) {
            System.out.println(job.getID());
        }
    }

    public <T extends Comparable<T>, K> Map<String, K> findLowestChoices(Map<String, T> map1, Map<String, K> map2, T lowestValue, T stop){
        Map<String, K> choices = new HashMap<>();

        //  find the lowest load
        Iterator<Map.Entry<String, T>> iterator = map1.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, T> entry1 = iterator.next();
            if (entry1.getValue().compareTo(lowestValue) < 0 && entry1.getValue().compareTo(stop) >= 0) {
                lowestValue = entry1.getValue();
            }
        }

        //  find all agents that have the lowest load
        iterator = map1.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, T> entry2 = iterator.next();
            if (entry2.getValue() == lowestValue) {
                choices.put(entry2.getKey(), map2.get(entry2.getKey()));
            }
        }

        return choices;
    }
}
