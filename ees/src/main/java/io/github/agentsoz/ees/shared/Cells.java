package io.github.agentsoz.ees.shared;

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
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import io.github.agentsoz.ees.util.Parser;
import io.github.agentsoz.util.CoordinateConversion;
import io.github.agentsoz.util.Location;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Cells {

    public static int[] resolutions = null;
    public static H3Core h3Core;

    static {
        try {
            h3Core = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int radius = 0;

    //  area
    static int areaAgentIdCounter = 0;
    public static ArrayList<String> areaAgentCells = new ArrayList<>();
    public static HashMap<String, String> cellAgentMap = new HashMap<>();

    private static String ZONE;

    //  trike
    public static HashMap<String, Location> trikeRegisterLocations = new HashMap<>();

    public static String locationToCellAddress(Location location, int resolution) {
        CoordinateConversion coordinateConversion = new CoordinateConversion();
        String UTM = Cells.ZONE + " " + location.x + " " + location.y;
        double[] latlng = coordinateConversion.utm2LatLon(UTM);
        double lat = latlng[0];
        double lng = latlng[1];

        return h3Core.latLngToCellAddress(lat, lng, resolution);
    }

    public static String findKey(Location location) {
        CoordinateConversion coordinateConversion = new CoordinateConversion();
        String UTM = Cells.ZONE + " " + location.x + " " + location.y;
        double[] latlng = coordinateConversion.utm2LatLon(UTM);
        double lat = latlng[0];
        double lng = latlng[1];

        String smallestCell = h3Core.latLngToCellAddress(lat, lng, resolutions[resolutions.length - 1]);

        for (int i = resolutions.length - 1; i >= 0; i--) {
            String address = h3Core.cellToParentAddress(smallestCell, resolutions[i]);

            if (cellAgentMap.containsKey(address)) {
                return address;
            }
        }

        return null;
    }

    public static int getCellResolution(String cell) {
        return h3Core.getResolution(cell);
    }

    public static List<String> getNeighbours(String origin) {
        List<String> neighbourIds = new ArrayList<>();
        for (String neighbourCell : h3Core.gridDisk(origin, radius)) {
            if (cellAgentMap.containsKey(neighbourCell) && !neighbourCell.equals(origin)) {
                neighbourIds.add(cellAgentMap.get(neighbourCell));
            }
        }
        return neighbourIds;
    }

    public static long getHops(String cell1, String cell2) {
        return h3Core.gridDistance(cell1, cell2);
    }

    public static void applyConfig(String path) {
        setTrikeLocations(path);
        setAreaAgentCells(path);
    }

    private static void setAreaAgentCells(String path) {
        Element doc = Parser.parseXML(path);
        Cells.ZONE = doc.getElementsByTagName("zone").item(0).getTextContent();
        Cells.radius = Integer.parseInt(doc.getElementsByTagName("radius").item(0).getTextContent());

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
            if (cellNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            areaAgentCells.add(cellNode.getTextContent());
            cellAgentMap.put(cellNode.getTextContent(), "area: " + cellAgentMap.size());
        }
    }

    private static void setTrikeLocations(String path) {
        Element configRoot = Parser.parseXML(path);
        String populationPath = configRoot.getElementsByTagName("population").item(0).getTextContent();
        Element doc = Parser.parseXML(populationPath);

        NodeList personNL = doc.getElementsByTagName("person");
        for (int i = 0; i < personNL.getLength(); i++) {
            Element personElement = (Element) personNL.item(i);
            Element activityElement = (Element) personElement.getElementsByTagName("activity").item(0);
            String id = personElement.getAttribute("id");

            Element cellsEl = (Element) configRoot.getElementsByTagName("cells").item(0);

            NodeList trikeAgentNL = cellsEl.getElementsByTagName("trikeagent");
            if (trikeAgentNL.getLength() != 0) {
                Element trikeAgent = (Element) trikeAgentNL.item(0);
                NodeList nodeList = trikeAgent.getElementsByTagName("num");

                int sum = 0;
                for (int j = 0; j < nodeList.getLength(); j++) {
                    sum += Integer.parseInt(nodeList.item(j).getTextContent());
                    if (sum > Integer.parseInt(id)) {
                        String areaCell = cellsEl.getElementsByTagName("cell").item(j).getTextContent();
                        trikeRegisterLocations.put(id, Cells.getCellLocation(areaCell));
                        break;
                    }
                }
            } else {
                double x = Double.parseDouble(activityElement.getAttribute("x"));
                double y = Double.parseDouble(activityElement.getAttribute("y"));
                trikeRegisterLocations.put(id, new Location("", x, y));
            }
        }
    }

    public static Location getCellLocation(String cell) {
        CoordinateConversion coordinateConversion = new CoordinateConversion();
        LatLng latLng = h3Core.cellToLatLng(cell);
        String[] utmArr = coordinateConversion.latLon2UTM(latLng.lat, latLng.lng).split(" ");
        return new Location("", Integer.parseInt(utmArr[2]), Integer.parseInt(utmArr[3]));
    }
}
