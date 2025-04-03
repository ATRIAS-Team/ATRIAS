package io.github.agentsoz.ees.centralplanner.util;

import io.github.agentsoz.ees.centralplanner.Graph.Graph;
import io.github.agentsoz.ees.centralplanner.Simulation.Vehicle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class Util {

    public static void showProgress(int currentIteration, int totalIterations) {
        int barLength = 50; // Length of the progress bar
        int progress = (currentIteration * 100) / totalIterations;
        int filled = (currentIteration * barLength) / totalIterations;

        // Build the progress bar
        String bar = "=".repeat(filled) + " ".repeat(barLength - filled);

        // Print the progress bar and percentage
        System.out.print("\r[" + bar + "] " + progress + "%");
    }

    public static void showProgress(int currentIteration, int totalIterations, String text) {
        int barLength = 50; // Length of the progress bar
        int progress = (currentIteration * 100) / totalIterations;
        int filled = (currentIteration * barLength) / totalIterations;

        // Build the progress bar
        String bar = "=".repeat(filled) + " ".repeat(barLength - filled);

        // Print the progress bar and percentage
        System.out.print("\r[" + bar + "] " + progress + "%");
        System.out.print(text);
    }

    public static void initializeOutputFolder(String outputFilePath){
        File directory = new File(outputFilePath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs(); // Creates the directory and any necessary parent directories
            if (created) {
                System.out.println("Directory created successfully: " + outputFilePath);
            } else {
                System.err.println("Failed to create directory: " + outputFilePath);
            }
        } else {
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        System.err.println("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public static HashMap<String, String> xmlConfigParser(String path) {
        HashMap<String, String> configMap= new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path);

            NodeList outerNodes = doc.getDocumentElement().getChildNodes();

            for (int i = 0; i < outerNodes.getLength(); i++) {
                Node outerNode = outerNodes.item(i);

                if (outerNode instanceof Element) {
                    Element elem = (Element) outerNode;

                    String outerNodeTagName = elem.getTagName();
                    NodeList innerNodes = elem.getChildNodes();

                    for (int j = 0; j < innerNodes.getLength(); j++) {
                        Node innerNode = innerNodes.item(j);

                        if (innerNode instanceof Element) {
                            switch (outerNodeTagName) {
                                case "scenario":
                                        configMap.put(innerNode.getNodeName(), innerNode.getTextContent());
                                        // go on the search for the network map filepath
                                        if (innerNode.getNodeName().equals("config")){
                                            HashMap<String, String> scenarioConfig = xmlConfigParser(innerNode.getTextContent());
                                            configMap.putAll(scenarioConfig);
                                        }
                                    break;
                                case "file":
                                    if (((Element) innerNode).getAttribute("target_path").equals("component->type=TrikeAgent")) {
                                        configMap.put("TrikeAgent", ((Element) innerNode).getAttribute("number"));
                                    }
                                    break;
                                case "class":
                                    String subkey = ((Element) innerNode).getAttribute("field_name");
                                    configMap.put(subkey, innerNode.getTextContent());
                                    break;
                                // for scenario config
                                case "models":
                                    NodeList modelNodes = innerNode.getChildNodes();
                                    for (int k = 0; k < modelNodes.getLength(); k++) {
                                        Node modelNode = modelNodes.item(k);
                                        if (modelNode instanceof Element) {
                                            String id = ((Element) modelNode).getAttribute("id");
                                            String value = ((Element) modelNode).getTextContent();
                                            if (id.equals("configXml")){
                                                configMap.put(id, value);
                                                HashMap<String, String> matsimConfig = xmlConfigParser(value);
                                                configMap.putAll(matsimConfig);
                                            }
                                        }
                                    }
                                    break;
                                // for matsim config
                                case "module":
                                    String subpath = path.substring(0, path.lastIndexOf("/")+1);
                                    if (((Element) innerNode).getAttribute("name").equals("inputPlansFile")) {
                                        configMap.put("inputPlansFile", subpath + ((Element) innerNode).getAttribute("value"));
                                    } else if (((Element) innerNode).getAttribute("name").equals("inputNetworkFile")) {
                                        configMap.put("inputNetworkFile", subpath + ((Element) innerNode).getAttribute("value"));
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configMap;
    }

    public static ArrayList<Vehicle> generateFromXmlFile(String path, Graph graph){
        HashMap<String, String> configMap = new HashMap<>();
        HashMap<Integer, List<String>> populationMap = new HashMap<>();
        ArrayList<Vehicle> vehicles = new ArrayList<>();

        try {
            // Parse XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(path));

            // Normalize the document
            document.getDocumentElement().normalize();

            // Read node
            NodeList nodeList = document.getElementsByTagName("file");
            configMap.put("POPULATION_FILE", ((Element) nodeList.item(0)).getAttribute("target_file"));
            configMap.put("VEHICLES", ((Element) ((Element) nodeList.item(1)).getElementsByTagName("component").item(0)).getAttribute("number"));
            NodeList nodeList2 = document.getElementsByTagName("field");
            for (int i=0;i<nodeList2.getLength();i++){
                Element element = (Element) nodeList2.item(i);
                configMap.put(element.getAttribute("field_name"), element.getTextContent());
            }

            Document populationDoc = builder.parse(new File(configMap.get("POPULATION_FILE")));
            populationDoc.getDocumentElement().normalize();

            NodeList popNodeList = populationDoc.getElementsByTagName("person");

            for (int i=0;i<popNodeList.getLength();i++){
                List<String> coords = new ArrayList<String>();
                String x = ((Element) popNodeList.item(i)).getElementsByTagName("activity").item(0).getAttributes().getNamedItem("x").getNodeValue();
                String y = ((Element) popNodeList.item(i)).getElementsByTagName("activity").item(0).getAttributes().getNamedItem("y").getNodeValue();
                coords.add(x);
                coords.add(y);
                populationMap.put(i, coords);
            }

            for (int i=0;i<Integer.parseInt(configMap.get("VEHICLES"));i++){
                String homeNode = graph.getNearestNodeID(populationMap.get(i).get(0), populationMap.get(i).get(1));
                Vehicle vehicle = new Vehicle(i, homeNode, Float.parseFloat(configMap.get("CHARGING_THRESHOLD")));
                vehicles.add(vehicle);
            }

            return vehicles;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

