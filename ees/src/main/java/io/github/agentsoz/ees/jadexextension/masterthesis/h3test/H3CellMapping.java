package io.github.agentsoz.ees.jadexextension.masterthesis.h3test;

import java.util.List;
import java.io.IOException;
import java.util.Hashtable;
import com.uber.h3core.H3Core;
import com.uber.h3core.H3CoreV3;
import com.uber.h3core.util.LatLng;
import com.uber.h3core.exceptions.H3Exception;
import com.uber.h3core.H3CoreLoader.OperatingSystem;

import io.github.agentsoz.util.Location;


//TODO: Innerhalb einer Bonding box, Auflistung der CellAddresses aller Zellen, die vorkommen.


public class H3CellMapping {

    public static String getZoneCell(Location location) throws IOException {

        //  1. H3 soll sagen, in welcher zone Location ist (entweder H3 Adresse als String oder long Zahl,
        //  hier mit String gearbeitet)
        //  2. Tabelle, die sagt für welche H3 Zone welcher Area Agent zuständig ist
        //  3. Aus der Tabelle dann Area Agent Name als String zurückgeben

        H3Core h3 = H3Core.newInstance(OperatingSystem.WINDOWS, "x64");
        //double lat = location.getX();
        //double lng = location.getY();
        int res = 3;

        //Methode gibt die Zell-Adresse an
        return h3.latLngToCellAddress(location.getX(), location.getY(), res);

    }

    public static List<LatLng> getZoneCellBoundary(String zoneCell) throws IOException{
        H3Core h3 = H3Core.newInstance(OperatingSystem.WINDOWS, "x64");
        return h3.cellToBoundary(zoneCell);

    }

    //public static Hashtable<String,String> checkZoneCell(Location location){
    //   Hashtable<String, String> areaAgentZones = new Hashtable<>();
    // areaAgentZones.put(h3Agent.getZoneCell(location), AreaAgentList)
    //}
    //

    public static Hashtable<String, String> createCellMapping() throws IOException {
        H3CoreV3 h3 = H3CoreV3.newInstance();
        Hashtable<String, String> cellMapping = new Hashtable<>();

        // Define your Zone Agent IDs and corresponding lat/lng coordinates
        String zoneAgentID1 = "Agent123";
        double lat1 = 37.7749; // Latitude
        double lng1 = -122.4194; // Longitude

        String zoneAgentID2 = "Agent456";
        double lat2 = 34.0522; // Latitude
        double lng2 = -118.2437; // Longitude

        //Convert lat/lng coordinates to H3 cell address
        String cellAddress1 = h3.geoToH3Address(lat1, lng1, 9); // Resolution 9
        String cellAddress2 = h3.geoToH3Address(lat2, lng2, 9);

        //Put the cell address and Zone Agent IDs into the Hashtable
        cellMapping.put(cellAddress1, zoneAgentID1);
        cellMapping.put(cellAddress2, zoneAgentID2);

        return cellMapping;
    }

    public static void main(String[] args) throws IOException {

        //H3Core h3 = H3Core.newInstance(OperatingSystem.WINDOWS, "x64");
        Location loc = new Location("", 37.7955, -122.3937);

        String sol = getZoneCell(loc);
        System.out.println(sol);


        List<LatLng> sol2 = getZoneCellBoundary(sol);
        System.out.println(sol2);

        //  double lat = loc.getX();
        //  double lng = loc.getY();
        //int res = 5;

        //String sol = h3.latLngToCellAddress(lat, lng, res);
        //System.out.println(sol);

        // LatLng latLng = new LatLng(lat, lng);
        // System.out.println(latLng);

        // long sol = h3.latLngToCell(lat, lng, res);
        // System.out.println(sol);

        //double sol = h3.latLngToCell(lat, lng, res);
        //System.out.println(sol);

        Hashtable<String, String> cellMapping = createCellMapping();
        H3Core h3 = H3Core.newInstance();

        //Print the contents of the Hashtable and cell boundaries
        for (String cellAddress : cellMapping.keySet()) {
            String zoneAgentID = cellMapping.get(cellAddress);

            //Convert cell address to boundary
            List<LatLng> boundary = h3.cellToBoundary(cellAddress);

            System.out.println("Cell Address: " + cellAddress + ", Zone Agent ID: " + zoneAgentID);
            System.out.println("Boundary: " + boundary);
        }
    }
}