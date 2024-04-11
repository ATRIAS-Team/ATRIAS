package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.io.FileWriter;
import java.io.IOException;

public class csvLogger {
    //public String filename;
    public String delimiter = ";";

    public boolean created = false;

    public csvLogger(){}



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
            writer = new FileWriter("LogAgent#" + agentID + "_CNP#" + CNP_ACTIVE + "_THETA#" + THETA + "_MISS#" + ALLOW_CUSTOMER_MISS + "_CH.THRES#" + CHARGING_THRESHOLD + "_COM.THRES#" + commitThreshold + "_DI.FACTOR#" + DISTANCE_FACTOR + ".csv", false);
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
            writer = new FileWriter("LogAgent#" + agentID + "_CNP#" + CNP_ACTIVE + "_THETA#" + THETA + "_MISS#" + ALLOW_CUSTOMER_MISS + "_CH.THRES#" + CHARGING_THRESHOLD + "_COM.THRES#" + commitThreshold + "_DI.FACTOR#" + DISTANCE_FACTOR + ".csv", true);
            writer.append(String.join( ";", "AgentID", "TripID", "DriveOperationNumber", "TripType",
                    "BatteryBefore", "BatteryAfter", "ArrivedAtLocation", "Distance", "arrivalTime", "Origin") + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addHeader(String agentID){
        FileWriter writer = null;
        try {
            writer = new FileWriter("LogAgent_" + agentID + ".csv", true);
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
            writer = new FileWriter("LogAgent#" + agentID + "_CNP#" + CNP_ACTIVE + "_THETA#" + THETA + "_MISS#" + ALLOW_CUSTOMER_MISS + "_CH.THRES#" + CHARGING_THRESHOLD + "_COM.THRES#" + commitThreshold + "_DI.FACTOR#" + DISTANCE_FACTOR + ".csv", true);
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