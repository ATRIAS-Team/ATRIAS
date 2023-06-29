package io.github.agentsoz.ees.jadexextension.masterthesis.Run;

import io.github.agentsoz.bdiabm.BDIServerInterface;
import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.ModelInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.DiffusedContent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.SimSensoryInputBroker;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.SimActuator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;


import java.util.*;

public class JadexModel implements BDIServerInterface, ModelInterface, DataClient, EnvironmentActionInterface, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JadexModel.class);


    private QueryPerceptInterface queryInterface;
    public static AgentDataContainer outAdcincycle;
    public static AgentDataContainer outAdcoutCycle;
    private DataServer dataServer;

    private static String eBDIAgentType = "BDIAgentType";

    public static SimActuator storageAgent;
    public static int finishedAreaAgent;

    public static List<String> answeredInputBroker = new ArrayList<>();

    //public static TrikeAgentMVP3 storageAgent;

    private final Map<String, DataClient> dataListeners = createDataListeners();

    private Object sequenceLock;
    public static final Object lock = new Object();

    public static final Object lock2 = new Object();

    public static int TrikeAgentnumber;
    public static int SimSensoryInputBrokernumber;

    public static double simulationtime;

    public static boolean inBDIcycle = true;





    public JadexModel(Map<String, String> opts, DataServer dataServer, QueryPerceptInterface qpi) {
        storageAgent = new SimActuator();
        //   storageAgent = new TrikeAgentMVP3();
        this.dataServer = dataServer;
        //  this.queryInterface = qpi;
        this.outAdcincycle = new AgentDataContainer();
        this.outAdcoutCycle = new AgentDataContainer();
        //  this.agentsInitMap = agentsInitMap;
        this.setQueryPerceptInterface(qpi);

    }

    public void registerDataServer(DataServer dataServer) {
        this.dataServer = dataServer;
    }

    @Override
    public void init(Object[] args) {
        dataServer.subscribe(this, Constants.TAKE_CONTROL_BDI);
        simulationtime = 0;


        //    logger.info("Initialising jill with args: " + Arrays.toString(initArgs)); Add Logger later
        // Set the BDI query percept interface for the storage Agent
        storageAgent.setQueryPerceptInterface(this.getQueryPerceptInterface());
        TrikeAgentnumber = (int) args[0];
        SimSensoryInputBrokernumber = (int) args[1];
    }



    @Override
    public void start() {
          TrikeMain.start();

          run2();

    }

    @Override
    public Object[] step(double time, Object[] args) {
        return new Object[0];
    }

    @Override
    public void finish() {

    }


    @Override
    public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
        this.queryInterface = queryInterface;

    }

    @Override
    public QueryPerceptInterface getQueryPerceptInterface() {
        return queryInterface;
    }

    @Override
    public void setAgentDataContainer(AgentDataContainer adc) {
        outAdcincycle = adc;
    }

    @Override
    public AgentDataContainer getAgentDataContainer() {
        return outAdcincycle;
    }


    @Override
    public AgentDataContainer takeControl(double time, AgentDataContainer inAdc) {
        answeredInputBroker = new ArrayList<>();
        simulationtime = time;
        if (inAdc != null) {
            //Depends on number of Area Agents to set value of NewDatafromMATSIM of all of them to true. at the moment we have 1
            SimSensoryInputBroker.NewDatafromMATSIM = true;
            SimSensoryInputBroker.inAdcMATSIM = inAdc;
            run();
            SimSensoryInputBroker.NewDatafromMATSIM = false;
            }


        return outAdcincycle;
    }

    @Override
    public void receiveData(double time, String dataType, Object data) {
            switch (dataType) {
                case Constants.TAKE_CONTROL_BDI:
                    dataListeners.get(dataType).receiveData(time, dataType, data);
                    break;
                default:
                    throw new RuntimeException("Unknown data type received: " + dataType);
            }
        }

    @Override
    public void packageAction(String agentId,
                              String actionId,
                              Object[] parameters,
                              String actionState) {
        ActionContent.State state = ActionContent.State.INITIATED;
        if (actionState != null) {
            try {
                state = ActionContent.State.valueOf(actionState);
            } catch (Exception e) {
                logger.warn("agent {} ignoring unknown action state {}", agentId, actionState);
            }
        }
        ActionContent ac = new ActionContent(parameters, state, actionId);
        outAdcincycle.putAction(agentId, actionId, ac);
    }

    private Map<String, DataClient> createDataListeners() {
        Map<String, DataClient> listeners = new  HashMap<>();
        listeners.put(Constants.TAKE_CONTROL_BDI, (DataClient<AgentDataContainer>) (time, dataType, data) -> {
            //takeControl(data);
            synchronized (getSequenceLock()) {
                getAgentDataContainer().clear();
                copy(outAdcoutCycle,outAdcincycle);
                outAdcoutCycle.clear();

                takeControl(time, data);
                dataServer.publish(Constants.AGENT_DATA_CONTAINER_FROM_BDI, getAgentDataContainer());
            }
        });


        return listeners;
    }

    protected Object getSequenceLock() {
        return sequenceLock;
    }

    public void useSequenceLock(Object sequenceLock) {
        this.sequenceLock = sequenceLock;
    }

    public static Object[] extractXMLData() {
        Object[] requiredBDIinfo = new Object[2];
        try {
            // creating a constructor of file class and
            // parsing an XML file


            File file = new File(
                    Config.pathname);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);

            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("configuration");

            for (int x = 0, size = nodeList.getLength(); x < size; x++) {
                Node node = nodeList.item(x);
                Element tElement = (Element) node;
                NodeList nodeList1 = tElement.getElementsByTagName("components");
                for (int y = 0, size2 = nodeList1.getLength(); y < size2; y++) {
                    Node node1 = nodeList1.item(y);
                    Element tElement1 = (Element) node1;
                    NodeList nodeList2 = tElement.getElementsByTagName("component");
                    for (int z = 0, size3 = nodeList2.getLength(); z < size3; z++) {
                        String componentname = nodeList2.item(z).getAttributes().getNamedItem("type").getNodeValue();
                        String componentnumber = nodeList2.item(z).getAttributes().getNamedItem("number").getNodeValue();
                        if (Objects.equals(componentname, "TrikeAgent")) {
                            requiredBDIinfo[0] = Integer.parseInt(componentnumber);
                        }
                        if (Objects.equals(componentname, "SimSensoryInputBroker")) {
                            requiredBDIinfo[1] = Integer.parseInt(componentnumber);
                        }

                    }


                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return requiredBDIinfo;
    }

    private void copy(AgentDataContainer from, AgentDataContainer to) {
        if (from != null) {
            Iterator<String> it = from.getAgentIdIterator();
            while (it.hasNext()) {
                String agentId = it.next();
                // Copy percepts
                Map<String, PerceptContent> percepts = from.getAllPerceptsCopy(agentId);
                for (String perceptId : percepts.keySet()) {
                    PerceptContent content = percepts.get(perceptId);
                    to.putPercept(agentId, perceptId, content);
                }
                // Copy actions
                Map<String, ActionContent> actions = from.getAllActionsCopy(agentId);
                for (String actionId : actions.keySet()) {
                    ActionContent content = actions.get(actionId);
                    to.putAction(agentId, actionId, content);
                }
            }
        }
    }

    @Override
    public void run() {
        synchronized (lock) {
            while (answeredInputBroker.size() != 2) {
                try {
                    lock.wait();

                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void run2() {
        synchronized (lock2) {
            while (TrikeMain.TrikeAgentNumber != TrikeAgentnumber) {
                try {
                    lock2.wait();

                } catch (InterruptedException e) {
                }
            }
        }
    }



    public static void flagMessage() {
        synchronized (lock) {
            lock.notify(); // NOPMD - ignore notifyall() warning
        }
    }

    public static void flagMessage2() {
        synchronized (lock2) {
            lock2.notify(); // NOPMD - ignore notifyall() warning
        }
    }

    public class TimedAlert {
        public double time;


        private TimedAlert(double time) {
            this.time = time;

        }

        private double getTime() {
            return time;
        }
    }
    }


