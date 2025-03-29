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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class csvLogger {
    //public String filename;
    public String delimiter = ";";

    public boolean created = false;

    public csvLogger(){}



    private static String getLogDirectory() {
        // Read directory from environment variable
        String configFile = System.getenv("ConfigFile").split("/")[1];
        String logDir = configFile.substring(0, configFile.length() - 4);

        // Ensure directory exists
        File dir = new File("output/" + logDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return "output/" + logDir + "/";
    }

    public csvLogger(String agentID){
        //his.filename = "LogAgent_" + agentID + ".csv";
        //this.delimiter = ";";
        FileWriter writer = null;
        try {
            writer = new FileWriter("LogAgent_" + agentID + ".csv", false);
            //writer.append(String.join(delimiter, row) + "\n");
            //writer.flush();
            addHeader(agentID);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public csvLogger(String agentID, Boolean CNP_ACTIVE, Double THETA, Boolean ALLOW_CUSTOMER_MISS, Double CHARGING_THRESHOLD, Double commitThreshold, Double DISTANCE_FACTOR){
        //his.filename = "LogAgent_" + agentID + ".csv";
        //this.delimiter = ";";
        FileWriter writer = null;
        try {
            writer = new FileWriter(getLogDirectory() + "LogAgent#" + agentID + "_CNP#" + CNP_ACTIVE + "_THETA#" + THETA + "_MISS#" + ALLOW_CUSTOMER_MISS + "_CH.THRES#" + CHARGING_THRESHOLD + "_COM.THRES#" + commitThreshold + "_DI.FACTOR#" + DISTANCE_FACTOR + ".csv", false);
            //writer.append(String.join(delimiter, row) + "\n");
            //writer.flush();
            addHeader(agentID, CNP_ACTIVE, THETA, ALLOW_CUSTOMER_MISS, CHARGING_THRESHOLD, commitThreshold, DISTANCE_FACTOR);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addHeader(String agentID, Boolean CNP_ACTIVE, Double THETA, Boolean ALLOW_CUSTOMER_MISS, Double CHARGING_THRESHOLD, Double commitThreshold, Double DISTANCE_FACTOR){
        FileWriter writer = null;
        try {
            writer = new FileWriter(getLogDirectory() + "LogAgent#" + agentID + "_CNP#" + CNP_ACTIVE + "_THETA#" + THETA + "_MISS#" + ALLOW_CUSTOMER_MISS + "_CH.THRES#" + CHARGING_THRESHOLD + "_COM.THRES#" + commitThreshold + "_DI.FACTOR#" + DISTANCE_FACTOR + ".csv", true);
            writer.append(String.join( ";", "AgentID", "TripID", "DriveOperationNumber", "TripType",
                    "BatteryBefore", "BatteryAfter", "ArrivedAtLocation", "Distance", "arrivalTime", "Origin", "Start_Pos", "End_Pos") + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addHeader(String agentID){
        FileWriter writer = null;
        try {
            writer = new FileWriter(getLogDirectory() + "LogAgent_" + agentID + ".csv", true);
            writer.append(String.join( ";", "AgentID", "TripID", "DriveOperationNumber", "TripType",
                    "BatteryBefore", "BatteryAfter", "ArrivedAtLocation", "Distance", "arrivalTime", "Origin") + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void addLog(String agentID, String... row){
        FileWriter writer = null;
        try {
            writer = new FileWriter("LogAgent_" + agentID + ".csv", true);
            writer.append(String.join(";", row) + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addLog(String agentID, Boolean CNP_ACTIVE, Double THETA, Boolean ALLOW_CUSTOMER_MISS, Double CHARGING_THRESHOLD, Double commitThreshold, Double DISTANCE_FACTOR, String... row){
        FileWriter writer = null;
        try {
            writer = new FileWriter(getLogDirectory() + "LogAgent#" + agentID + "_CNP#" + CNP_ACTIVE + "_THETA#" + THETA + "_MISS#" + ALLOW_CUSTOMER_MISS + "_CH.THRES#" + CHARGING_THRESHOLD + "_COM.THRES#" + commitThreshold + "_DI.FACTOR#" + DISTANCE_FACTOR + ".csv", true);
            writer.append(String.join(";", row) + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }








/**
    public static void addRow(String... row){


        FileWriter writer = null;
        try {
            writer = new FileWriter(filename, true);
            writer.append(String.join(delimiter, row) + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
**/

}
