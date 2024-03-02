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




        //addRow("AgentID", "TripID", "DriveOperationNumber", "TripType",
          //      "BatteryBefore", "BatteryAfter", "ArrivedAtLocation", "Distance", "Origin");
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