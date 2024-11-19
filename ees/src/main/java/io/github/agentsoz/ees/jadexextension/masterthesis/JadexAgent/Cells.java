package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import io.github.agentsoz.util.CoordinateConversion;
import io.github.agentsoz.util.Location;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Cells {
    public static int[] resolutions = null;


    //  area
    static int areaAgentIdCounter = 0;
    static ArrayList<String> areaAgentCells = new ArrayList<>();
    static HashMap<String, String> cellAgentMap = new HashMap<>();

    //  trike
    static HashMap<String, Location> trikeRegisterLocations = new HashMap<>();

    public static String locationToCellAddress(Location location, int resolution) {
        CoordinateConversion coordinateConversion = new CoordinateConversion();

        String UTM = "32 U " + location.x + " " + location.y;
        double[] latlng = coordinateConversion.utm2LatLon(UTM);
        double lat = latlng[0];
        double lng = latlng[1];

        H3Core h3 = null;
        try {
            h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return h3.latLngToCellAddress(lat, lng, resolution);
    }

    public static String findKey(Location location){
        CoordinateConversion coordinateConversion = new CoordinateConversion();

        String UTM = "32 U " + location.x + " " + location.y;
        double[] latlng = coordinateConversion.utm2LatLon(UTM);
        double lat = latlng[0];
        double lng = latlng[1];

        H3Core h3 = null;
        try {
            h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String smallestCell = h3.latLngToCellAddress(lat, lng, resolutions[resolutions.length - 1]);

        for (int i = resolutions.length - 1; i >= 0; i--){
            String address = h3.cellToParentAddress(smallestCell, resolutions[i]);
            LatLng latLng = h3.cellToLatLng(address);

            if(cellAgentMap.containsKey(address)) return address;
        }

        return null;
    }

    public static int getCellResolution(String cell){
        H3Core h3 = null;
        try {
            h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return h3.getResolution(cell);
    }



    public static void applyConfig(){
        setTrikeLocations();
        setAreaAgentCells();
    }

    private static void setAreaAgentCells(){
        String path = "XMLConfig.xml";
        Element doc = Parsers.parseXML(path);
        Node areaAgentNode = doc.getElementsByTagName("areaagent").item(0);
        String[] resolutionsStrArr = areaAgentNode
                .getParentNode()
                .getAttributes()
                .getNamedItem("resolutions")
                .getNodeValue().split(",");

        resolutions = Arrays.stream(resolutionsStrArr)
                .mapToInt(Integer::parseInt)
                .toArray();

        NodeList cellsNL = areaAgentNode.getChildNodes();
        for (int i = 0; i < cellsNL.getLength(); i++) {
            Node cellNode = cellsNL.item(i);
            if(cellNode.getNodeType() != Node.ELEMENT_NODE) continue;
            areaAgentCells.add(cellNode.getTextContent());
        }
    }

    private static void setTrikeLocations(){
        String path = "ees/scenarios/matsim-boston/boston-population.xml";
        Element doc = Parsers.parseXML(path);

        NodeList personNL = doc.getElementsByTagName("person");
        for (int i = 0; i < personNL.getLength(); i++) {
            Element personElement = (Element) personNL.item(i);
            Element activityElement = (Element) personElement.getElementsByTagName("activity").item(0);
            String id = personElement.getAttribute("id");
            double x = Double.parseDouble(activityElement.getAttribute("x"));
            double y = Double.parseDouble(activityElement.getAttribute("y"));
            trikeRegisterLocations.put(id, new Location("", x, y));
        }
    }
}
