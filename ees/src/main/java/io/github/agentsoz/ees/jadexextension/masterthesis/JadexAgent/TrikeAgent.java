package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;
import com.google.api.core.ApiFuture;
import com.google.firebase.database.*;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.TrikeAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.ees.util.RingBuffer;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.Time;
import jadex.bdiv3.runtime.IPlan;
import jadex.commons.future.IFuture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService.messageToService;
import static io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig.getClassField;

@Agent(type= BDIAgentFactory.TYPE)
@ProvidedServices({
        @ProvidedService(type= IMappingAgentsService.class, implementation=@Implementation(WritingIDService.class)),
        @ProvidedService(type= INotifyService.class, implementation=@Implementation(TrikeAgentReceiveService.class)),
        @ProvidedService(type= INotifyService2.class, implementation=@Implementation(TrikeAgentSendService.class)),
        @ProvidedService(type= IAreaTrikeService.class, implementation=@Implementation( TrikeAgentService.class)),
})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name ="sendareaagendservice", type = IAreaTrikeService.class),
        @RequiredService(name="mapservices", type= IMappingAgentsService.class),
        @RequiredService(name="broadcastingservices", type= INotifyService.class, scope= ServiceScope.PLATFORM),
        @RequiredService(name="notifywhenexecutiondoneservice", type= INotifyService2.class, scope= ServiceScope.PLATFORM),
})

public class TrikeAgent implements SendtoMATSIM{
    public List<Location> AGENT_SPAWN_LOCATIONS = Arrays.asList(
            new Location("", 330129.503, 4690968.364),
            new Location("", 330130.503, 4690969.364),
            new Location("", 330131.503, 4690970.364),
            new Location("", 330132.503, 4690971.364),
            new Location("", 329969.702, 4690968.243),
            new Location("", 329970.702, 4690969.243),
            new Location("", 329971.702, 4690970.243),
            new Location("", 329972.702, 4690971.243),
            new Location("", 330226.178, 4690808.802),
            new Location("", 330227.178, 4690809.802),
            new Location("", 330228.178, 4690810.802),
            new Location("", 330229.178, 4690811.802),
            new Location("", 330394.635, 4691190.930),
            new Location("", 330395.635, 4691191.930),
            new Location("", 330396.635, 4691192.930),
            new Location("", 330397.635, 4691193.930),
            new Location("", 330019.125, 4690675.274),
            new Location("", 330020.125, 4690676.274),
            new Location("", 330021.125, 4690677.274),
            new Location("", 330022.125, 4690678.274),
            new Location("", 329830.824, 4690748.290),
            new Location("", 329831.824, 4690749.290),
            new Location("", 329832.824, 4690750.290),
            new Location("", 329833.824, 4690751.290),
            new Location("", 330061.408, 4691249.231),
            new Location("", 330062.408, 4691250.231),
            new Location("", 330063.408, 4691251.231),
            new Location("", 330064.408, 4691252.231),
            new Location("", 330085.391, 4690790.884),
            new Location("", 330086.391, 4690791.884),
            new Location("", 330087.391, 4690792.884),
            new Location("", 330088.391, 4690793.884),
            new Location("", 329865.114, 4690515.672),
            new Location("", 330562.816, 4691951.597),
            new Location("", 328339.214, 4686309.690),
            new Location("", 328914.473, 4689722.320),
            new Location("", 330121.529, 4685666.860),
            new Location("", 332336.418, 4689036.823),
            new Location("", 331374.527, 4689493.447),
            new Location("", 328554.314, 4691050.880),
            new Location("", 328408.376, 4690291.468),
            new Location("", 329384.471, 4689911.517),
            new Location("", 330748.810, 4689896.080),
            new Location("", 330841.656, 4691402.006),
            new Location("", 329939.713, 4691704.073),
            new Location("", 329987.630, 4692072.121),
            new Location("", 328400.333, 4692736.850),
            new Location("", 330253.355, 4690729.156),
            new Location("", 329546.587, 4690679.550),
            new Location("", 330669.230, 4692572.359),
            new Location("", 328664.301, 4692715.827),
            new Location("", 327443.488, 4687886.430),
            new Location("", 329457.517, 4686765.274),
            new Location("", 328575.172, 4687419.778),
            new Location("", 326951.879, 4688746.432),
            new Location("", 329914.806, 4685005.340),
            new Location("", 329210.623, 4685766.426),
            new Location("", 329146.197, 4687183.842),
            new Location("", 330653.494, 4688786.536),
            new Location("", 331421.452, 4689573.203),
            new Location("", 331002.342, 4692271.983),
            new Location("", 332789.342, 4692468.604),
            new Location("", 329070.509, 4686693.813),
            new Location("", 330279.417, 4686925.322),
            new Location("", 329864.114, 4690514.672),
            new Location("", 330561.816, 4691949.597),
            new Location("", 328337.214, 4686307.690),
            new Location("", 328912.473, 4689720.320),
            new Location("", 330119.529, 4685664.860),
            new Location("", 332334.418, 4689034.823),
            new Location("", 331372.527, 4689491.447),
            new Location("", 328552.314, 4691048.880),
            new Location("", 328406.376, 4690289.468),
            new Location("", 329382.471, 4689909.517),
            new Location("", 330746.810, 4689894.080),
            new Location("", 330839.656, 4691400.006),
            new Location("", 329937.713, 4691702.073),
            new Location("", 329985.630, 4692070.121),
            new Location("", 328398.333, 4692734.850),
            new Location("", 330251.355, 4690727.156),
            new Location("", 329544.587, 4690677.550),
            new Location("", 330667.230, 4692570.359),
            new Location("", 328662.301, 4692713.827),
            new Location("", 327441.488, 4687884.430),
            new Location("", 329455.517, 4686763.274),
            new Location("", 328573.172, 4687417.778),
            new Location("", 326949.879, 4688744.432),
            new Location("", 329912.806, 4685003.340),
            new Location("", 329208.623, 4685764.426),
            new Location("", 329144.197, 4687181.842),
            new Location("", 330651.494, 4688784.536),
            new Location("", 331419.452, 4689571.203),
            new Location("", 331000.342, 4692269.983),
            new Location("", 332787.342, 4692466.604),
            new Location("", 329068.509, 4686691.813),
            new Location("", 330277.417, 4686923.322),
            new Location("", 329868.114, 4690518.672),
            new Location("", 330565.816, 4691956.597),
            new Location("", 328342.214, 4686312.690),
            new Location("", 328917.473, 4689725.320),
            new Location("", 330124.529, 4685669.860),
            new Location("", 332339.418, 4689039.823),
            new Location("", 331377.527, 4689496.447),
            new Location("", 328557.314, 4691053.880),
            new Location("", 328411.376, 4690294.468),
            new Location("", 329387.471, 4689914.517),
            new Location("", 330751.810, 4689899.080),
            new Location("", 330844.656, 4691405.006),
            new Location("", 329942.713, 4691707.073),
            new Location("", 329990.630, 4692075.121),
            new Location("", 328403.333, 4692739.850),
            new Location("", 330256.355, 4690732.156),
            new Location("", 329549.587, 4690682.550),
            new Location("", 330672.230, 4692575.359),
            new Location("", 328667.301, 4692718.827),
            new Location("", 327446.488, 4687889.430),
            new Location("", 329460.517, 4686768.274),
            new Location("", 328578.172, 4687422.778),
            new Location("", 326954.879, 4688749.432),
            new Location("", 329917.806, 4685008.340),
            new Location("", 329213.623, 4685769.426),
            new Location("", 329149.197, 4687186.842),
            new Location("", 330656.494, 4688789.536),
            new Location("", 331424.452, 4689576.203),
            new Location("", 331005.342, 4692274.983),
            new Location("", 332792.342, 4692471.604),
            new Location("", 329073.509, 4686696.813),
            new Location("", 330282.417, 4686928.322)
    );
    /**
     public List<Location> AGENT_SPAWN_LOCATIONS = Arrays.asList(
     new Location("", 476693.70,5553399.74),
     new Location("", 476411.90963429067, 5552419.709277404),
     new Location("", 476593.32115363394, 5553317.19412722),
     new Location("", 476438.79189037136, 5552124.30651799),
     new Location("", 476500.76932398824, 5552798.971484745),
     new Location("", 476538.9427888916, 5553324.827033389),
     new Location("", 476619.6161561999, 5552925.794018047),
     new Location("", 476606.7547, 5552369.86),
     new Location("", 476072.454, 5552737.847),
     new Location("", 476183.6117, 5552372.253),
     new Location("", 476897.6661, 5552908.159),
     new Location("", 476117.4177, 5552983.103),
     new Location("", 476206.3887, 5553181.409),
     new Location("", 476721.5633, 5553163.268),
     new Location("", 476504.8636, 5553075.586),
     new Location("", 476006.3971, 5552874.791),
     new Location("", 476896.9427, 5552809.207),
     new Location("", 476576.8201, 5552875.558),
     new Location("", 476659.5715, 5552264.147),
     new Location("", 476140.0289, 5552869.111),
     new Location("", 476459.8442, 5552766.704),
     new Location("", 476076.6989, 5552496.082),
     new Location("", 475950.8911, 5553012.783),
     new Location("", 476269.0866, 5553041.63),
     new Location("", 476574.3644, 5552706.306),
     new Location("", 476229.5433, 5553032.162),
     new Location("", 476182.5081, 5552736.953),
     new Location("", 476718.9972, 5552412.517),
     new Location("", 476088.6448, 5552928.079),
     new Location("", 476285.4132, 5552547.373),
     new Location("", 476257.686, 5553038.9),
     new Location("", 476276.6184, 5553043.434)
     );
     */
    //Boston station
    public List<Location> CHARGING_STATION_LIST = Arrays.asList(
            new Location("", 330129.503,4690968.364),
            new Location("", 329969.702, 4690968.243),
            new Location("", 330226.178, 4690808.802),
            new Location("", 330394.635, 4691190.930),
            new Location("", 330019.125, 4690675.274),
            new Location("", 329830.824, 4690748.290),
            new Location("", 330061.408, 4691249.231),
            new Location("", 330085.391, 4690790.884),
            new Location("", 330376.760, 4691775.234),
            new Location("", 329510.461, 4690211.875),
            new Location("", 329947.761, 4690811.336),
            new Location("",330734.09049771406, 4686338.209947874),
            new Location("",331315.3926859213, 4686003.14675877),
            new Location("",330459.654522083, 4686797.65147697),
            new Location("",330522.2126332774, 4686923.635164486),
            new Location("",330404.0268183075, 4685725.674994842),
            new Location("",330267.0424466077, 4685598.519795385),
            new Location("",330162.4159192683, 4685455.758060012),
            new Location("",330934.30093666096, 4684885.481330267),
            new Location("",331067.9425282917, 4684873.356880021),
            new Location("",331458.9572182546, 4683871.1501955),
            new Location("",331525.34854709206, 4683418.775171504),
            new Location("",327719.550770866, 4685531.510325233),
            new Location("",327479.17367278016, 4686069.85136981),
            new Location("",326790.8477667136, 4689528.776679865),
            new Location("",326567.774762258, 4689664.697967572),
            new Location("",327710.277554634, 4689368.639028133),
            new Location("",327973.19664333365, 4689270.511257319),
            new Location("",328133.97907932295, 4689478.039451958),
            new Location("",328190.7759946394, 4689772.728762476),
            new Location("",326281.4664791448, 4689675.283094306),
            new Location("",328265.46399824356, 4690081.059870234),
            new Location("",328040.2401779549, 4690128.885234285),
            new Location("",327987.49280368455, 4690429.764286722),
            new Location("",328461.5517205638, 4690143.2098028455),
            new Location("",328831.32978277106, 4690507.727442217),
            new Location("",329767.2607187112, 4690445.978121304),
            new Location("",330225.55006005947, 4690092.84661864),
            new Location("",330912.8591505459, 4689999.019060219),
            new Location("",331006.74102454877, 4690518.329129274),
            new Location("",330983.7125710095, 4690296.8628525175),
            new Location("",326752.3580612396, 4692458.707911453),
            new Location("",326496.2080125784, 4692683.370884217),
            new Location("",329044.6136624153, 4693086.086950928),
            new Location("",329672.0546194103, 4692140.789613456),
            new Location("",330351.5084426627, 4692166.455323417),
            new Location("",331075.2075612657, 4693423.9441094035),
            new Location("",332053.42006065475, 4693016.88156659),
            new Location("",332260.02298745513, 4692919.660061572),
            new Location("",332283.69022679795, 4692945.753122068),
            new Location("",330773.91964123806, 4691695.911588468),
            new Location("",330541.70079147693, 4692117.668885517),
            new Location("",331125.97199872, 4692204.9227484055),
            new Location("",329529.3681031796, 4687436.116361706),
            new Location("",327010.8782558239, 4687482.809660871),
            new Location("",327044.18152553355, 4687003.280819914),
            new Location("",327065.7802733871, 4687885.381320049),
            new Location("",328175.1632187184, 4688539.366192844),
            new Location("",329529.4398850673, 4688287.695682363),
            new Location("",329364.698601429, 4688423.9164145),
            new Location("",330077.15229866316, 4687961.080699825),
            new Location("",329202.0219202008, 4683190.730182893),
            new Location("",328456.1722722984, 4682856.789983864),
            new Location("",327991.48788313573, 4683102.576120801),
            new Location("",328556.58158344286, 4683689.950927951),
            new Location("",329198.0303689931, 4684123.306078233),
            new Location("",329952.6582628207, 4684269.552173864),
            new Location("",328692.95462842024, 4684355.114709307),
            new Location("",328693.0153561778, 4684357.607604435),
            new Location("",327943.6362818504, 4684632.793215642),
            new Location("",328678.7676101875, 4684594.920629536),
            new Location("",329081.35585095023, 4685113.209289537),
            new Location("",328699.8707663481, 4685580.953568669),
            new Location("",328600.20644835127, 4685703.284318363)
    );

    //Goethe stations
    /**
     public List<Location> CHARGING_STATION_LIST = Arrays.asList(
     new Location("", 476142.33,5553197.70),
     new Location("", 476172.65,5552839.64),
     new Location("", 476482.10,5552799.06),
     new Location("", 476659.13,5553054.12),
     new Location("", 476787.10,5552696.95),
     new Location("", 476689.45,5552473.11),
     new Location("", 476405.41,5552489.17),
     new Location("", 476100.86,5552372.79)
     );
     */

    /**
     * The bdi agent. Automatically injected
     */
    @Agent
    private IInternalAccess agent;
    @AgentFeature
    protected IBDIAgentFeature bdiFeature;
    @AgentFeature
    protected IExecutionFeature execFeature;
    @AgentFeature
    protected IRequiredServicesFeature requiredServicesFeature;

    // to indicate if the agent is available to take the new ride
    @Belief
    public volatile boolean isMatsimFree;
    @Belief
    public boolean canExecute = true;

    @Belief
    public Location agentLocation;

    @Belief
    public List<DecisionTask> decisionTaskList = new ArrayList<>();

    @Belief
    public List<DecisionTask> FinishedDecisionTaskList = new ArrayList<>();

    @Belief
    public List<Trip> tripList = new ArrayList<>(); //contains all the trips

    @Belief
    public List<String> tripIDList = new ArrayList<>();

    @Belief    //contains the current Trip
    public List<Trip> currentTrip = new ArrayList<>();

    public RingBuffer<ActionContent> actionContentRingBuffer = new RingBuffer<>(16);

    public RingBuffer<PerceptContent> perceptContentRingBuffer = new RingBuffer<>(16);



    @Belief
    public String agentID = null; // store agent ID from the map
    @Belief
    public boolean sent = false;
    @Belief
    public String write = null;

    public Integer chargingTripCounter = 0;

    @Belief
    protected boolean daytime; //Battery -oemer
    @Belief
    public double sumLinkLength = 0.0;
    @Belief
    public BatteryModel trikeBattery = new BatteryModel();
    @Belief
    public List<Double> estimateBatteryAfterTIP = Arrays.asList(trikeBattery.getMyChargestate());

    public String currentSimInputBroker;
    private SimActuator SimActuator;

    @Belief
    public String chargingTripAvailable = "0"; //Battery -oemer

    public Double commitThreshold = 50.0;
    Double DRIVING_SPEED = 6.0;
    Boolean CNP_ACTIVE = true;
    Double THETA = 1000.0; //allowed waiting time for customers.
    Boolean ALLOW_CUSTOMER_MISS = true; // customer will miss when delay > THETA
    Double DISTANCE_FACTOR = 3.0; //multiply with distance estimations for energyconsumption, to avoid an underestimation

    Double CHARGING_THRESHOLD = 0.4; // Threshold to determine when a ChargingTrip should be generated

    boolean FIREBASE_ENABLED = false;

    public MyLogger logger;

    //  FIREBASE
    @Belief
    private FirebaseHandler<TrikeAgent, Trip> firebaseHandler;
    private HashMap<String, ChildEventListener> listenerHashMap;

    @Belief
    public String oldCellAddress = null;

    @Belief
    public String newCellAddress = null;

    /**
     * The agent body.
     */
    @OnStart
    public void body() {
        Element classElement = XMLConfig.getClassElement("TrikeAgent.java");
        configure(classElement);

        if(FIREBASE_ENABLED){
            firebaseHandler = new FirebaseHandler<>(this, tripList);
            listenerHashMap = new HashMap<>();
        }

        System.out.println("TrikeAgent sucessfully started;");
        SimActuator = new SimActuator();
        SimActuator.setQueryPerceptInterface(JadexModel.storageAgent.getQueryPerceptInterface());
        AddAgentNametoAgentList(); // to get an AgentID later
        isMatsimFree = true;
        bdiFeature.dispatchTopLevelGoal(new ReactoAgentIDAdded());
        bdiFeature.dispatchTopLevelGoal(new MaintainManageJobs());
        bdiFeature.dispatchTopLevelGoal(new TimeTest());
        bdiFeature.dispatchTopLevelGoal(new PerformSIMReceive());
        bdiFeature.dispatchTopLevelGoal(new MaintainTripService());
        //bdiFeature.dispatchTopLevelGoal(new LogSimActionList());
        //bdiFeature.dispatchTopLevelGoal(new LogSimPerceptList());
    }

    /**
     * This is just a debug Goal that will print many usefull information every 10s
     * TODO: find a better name
     */
    @Goal(recur=true, recurdelay=10000)
    class TimeTest {
    }

    @Plan(trigger=@Trigger(goals=TimeTest.class))
    private void TimeTestPrint()
    {
        System.out.println("agentID " + agentID +  " simtime" + JadexModel.simulationtime);
        Status();
    }

    @Goal(recur = true, recurdelay = 100)
    public class MaintainBatteryLoaded {
        @GoalCreationCondition(factchanged = "estimateBatteryAfterTIP") //
        public MaintainBatteryLoaded() {
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainBatteryLoaded.class))
    public void NewChargingTrip() {
        {
            if (estimateBatteryAfterTIP.get(0) < CHARGING_THRESHOLD && chargingTripAvailable.equals("0")){
                //estimateBatteryAfterTIP();
                chargingTripCounter+=1;
                String tripID = "CH";
                tripID = tripID.concat(Integer.toString(chargingTripCounter));
                Trip chargingTrip = new Trip(tripID, "ChargingTrip", getNextChargingStation(), "NotStarted");
                tripList.add(chargingTrip);
                tripIDList.add("1");
                chargingTripAvailable = "1";
            }
        }
    }

    public Location getNextChargingStation(){
        //CHARGING_STATION_LIST
        Location ChargingStation = CHARGING_STATION_LIST.get(0); //= new Location("", 476530.26535798033, 5552438.979076344);
        // last trip In pipe endlocation oder agentposition als ausgang nehmen
        Location startPosition;
        if (tripList.size() == 0 && currentTrip.size() == 0){
            startPosition = agentLocation;
        }
        else {
            startPosition = getLastTripInPipeline().getEndPosition();
        }
        Double lowestDistance = Double.MAX_VALUE;
        for (int i=0; i < CHARGING_STATION_LIST.size(); i++){
            Double compareDistance = Location.distanceBetween(startPosition, CHARGING_STATION_LIST.get(i));
            if (compareDistance<lowestDistance){
                lowestDistance = compareDistance;
                ChargingStation = CHARGING_STATION_LIST.get(i);
            }
        }

        return ChargingStation;
    }

    /**
     * Will generate Trips from the Jobs sent by the Area Agent
     */
    @Goal(recur=true, recurdelay=1000)
    class MaintainManageJobs
    {
        @GoalMaintainCondition
        boolean isDecisionEmpty()
        {
            return decisionTaskList.isEmpty();
        }
    }

    @Plan(trigger=@Trigger(goals=MaintainManageJobs.class))
    private void EvaluateDecisionTask()
    {
        boolean finishedForNow = false;
        while (!finishedForNow) {
            Integer changes = 0;
            for (int i = 0; i < decisionTaskList.size(); i++) {
                Integer currentChanges = selectNextAction(i);
                //progress abgreifen
                // funktion aufrufen
                //finished decisiontask List anlegen?
                //wenn durchlauf ohen Änderungen finishedForNow = true
                changes += currentChanges;
            }
            if(changes==0){
                finishedForNow = true;
            }
        }
    }

    public Integer selectNextAction(int index){
        int changes = 0;
        DecisionTask currentDecisionTask = decisionTaskList.get(index);

        switch (currentDecisionTask.getStatus()) {
            case "new": {
                //  Execute Utillity here > "commit"|"delegate"

                Double ownScore = calculateUtility(currentDecisionTask);
                currentDecisionTask.setUtillityScore(agentID, ownScore);
                if (ownScore < commitThreshold && CNP_ACTIVE) {
                    currentDecisionTask.setStatus("delegate");
                } else {
                    currentDecisionTask.setStatus("commit");
                    String timeStampBooked = new SimpleDateFormat("HH.mm.ss.ms").format(new java.util.Date());
                    System.out.println("FINISHED Negotiation - JobID: " + currentDecisionTask.getJobID() + " TimeStamp: " + timeStampBooked);
                }
                changes += 1;
                break;
            }
            case "commit": {
                //  create trip here

                DecisionTask dTaToTrip = currentDecisionTask;
                Trip newTrip = new Trip(currentDecisionTask, dTaToTrip.getIDFromJob(), "CustomerTrip", dTaToTrip.getVATimeFromJob(), dTaToTrip.getStartPositionFromJob(), dTaToTrip.getEndPositionFromJob(), "NotStarted");
                //TODO: create a unique tripID
                tripList.add(newTrip);

                if(FIREBASE_ENABLED){
                    //  listen to the new child in firebase
                    ChildEventListener childEventListener = firebaseHandler.childAddedListener("trips/"+newTrip.tripID, (dataSnapshot, previousChildName, list)->{
                        System.out.println(dataSnapshot);
                        // Iterate through messages under this trip
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            String message = (String) messageSnapshot.child("message").getValue();

                            // Check if the message is the specific question
                            if ("How many trips are scheduled before mine?".equals(message)) {
                                DatabaseReference parent = dataSnapshot.getRef().getParent();
                                String tripId = parent.getKey();
                                int numberOfTrips = 0;
                                System.out.println(list);
                                for(int i = 0; i < list.size(); i++){
                                    if(list.get(i).getTripID().equals(tripId)){
                                        numberOfTrips = i;
                                        break;
                                    }
                                }


                                // Push a new message node under the 'messages' node
                                DatabaseReference newMessageRef = dataSnapshot.getRef().push();

                                // Set the message content
                                newMessageRef.child("message").setValueAsync("Number of trips before yours: " + numberOfTrips);

                                // Add other necessary fields, e.g., sender and timestamp if needed
                                newMessageRef.child("sender").setValueAsync("agent");


                                // Remove the question message from Firebase
                                messageSnapshot.getRef().removeValue(new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            // Handle the error
                                            System.err.println("Error removing question message: " + databaseError.getMessage());
                                        }
                                    }
                                });
                            }
                        }
                    });

                    listenerHashMap.put(newTrip.getTripID(), childEventListener);


                    //test
                    DatabaseReference tripRef = FirebaseDatabase.getInstance().getReference().child("trips").child(newTrip.tripID).child("messages");

                    // Push a new message node under the 'messages' node
                    DatabaseReference newMessageRef = tripRef.push();

                    // Set the message content synchronously
                    try {
                        ApiFuture<Void> messageFuture = newMessageRef.child("message").setValueAsync("How many trips are scheduled before mine?");
                        messageFuture.get(); // Wait for the setValueAsync operation to complete
                        System.out.println("Message set successfully.");
                    } catch (Exception e) {
                        System.err.println("Error setting message: " + e.getMessage());
                    }
                }

                tripIDList.add("1");
                estimateBatteryAfterTIP();

                currentDecisionTask.setStatus("committed");
                FinishedDecisionTaskList.add(dTaToTrip);
                decisionTaskList.remove(index);

                if(FIREBASE_ENABLED){
                    FirebaseHandler.assignAgentToTripRequest(newTrip.getTripID(), agentID);
                }

                changes += 1;
                break;
            }
            case "delegate": {
                // start cnp here > "waitingForNeighbourlist"
                //TODO: neighbour request here
                //TODO: adapt
                // TEST MESSAGE DELETE LATER
                //bool makes sure that the methods below are called only once

                ArrayList<String> values = new ArrayList<>();
                values.add(currentDecisionTask.getJobID()); //todo move into a method
                decisionTaskList.get(index).setStatus("waitingForNeighbours");
                sendMessage("area:0", "request", "callForNeighbours", values);

                //sendTestAreaAgentUpdate();
                //testTrikeToTrikeService();
                //testAskForNeighbours();
                changes += 1;
                break;
            }
            case "readyForCFP": {
                /**
                 *  send cfp> "waitingForProposals"
                 */
                Job JobForCFP = currentDecisionTask.getJob();
                ArrayList<String> neighbourIDs = currentDecisionTask.getNeighbourIDs();
                for (int i = 0; i < neighbourIDs.size(); i++) {
                    //todo: klären message pro nachbar evtl mit user:
                    //todo: action values definieren
                    // values: gesammterJob evtl. bereits in area zu triek so vorhanden?
                    //sendMessageToTrike(neighbourIDs.get(i), "CallForProposal", "CallForProposal", JobForCFP.toArrayList());
                    testTrikeToTrikeService(neighbourIDs.get(i), "CallForProposal", "CallForProposal", JobForCFP.toArrayList());
                }

                currentDecisionTask.setStatus("waitingForProposals");
                changes += 1;
                break;
            }
            case "waitingForProposals": {
                //todo: überprüfen ob bereits alle gebote erhalten
                // falls ja ("readyForDecision")
                //todo:
                if (currentDecisionTask.testAllProposalsReceived()) {
                    decisionTaskList.get(index).setStatus("readyForDecision");
                }
                break;
            }
            case "readyForDecision": {
                /**
                 *  send agree/cancel > "waitingForConfirmations"
                 */
                currentDecisionTask.tagBestScore(agentID);
                for (int i = 0; i < currentDecisionTask.getUTScoreList().size(); i++) {
                    String bidderID = currentDecisionTask.getUTScoreList().get(i).getBidderID();
                    String tag = currentDecisionTask.getUTScoreList().get(i).getTag();
                    switch (tag) {
                        case "AcceptProposal": {
                            ArrayList values = new ArrayList<>();
                            values.add(currentDecisionTask.getJobID());
                            testTrikeToTrikeService(bidderID, tag, tag, values);
                            currentDecisionTask.setStatus("waitingForConfirmations");
                            break;
                        }
                        case "RejectProposal": {
                            ArrayList values = new ArrayList<>();
                            values.add(currentDecisionTask.getJobID());
                            testTrikeToTrikeService(bidderID, tag, tag, values);
                            break;
                        }
                        case "AcceptSelf": {
                            //todo: selbst zusagen
                            currentDecisionTask.setStatus("commit");
                            String timeStampBooked = new SimpleDateFormat("HH.mm.ss.ms").format(new java.util.Date());
                            System.out.println("FINISHED Negotiation - JobID: " + currentDecisionTask.getJobID() + " TimeStamp: " + timeStampBooked);
                            break;
                        }
                        default: {
                            //todo: print ungültiger tag
                            System.out.println(agentID + ": invalid UTScoretag");
                            break;
                        }
                    }
                }
                changes += 1;
                break;
            }
            case "readyForConfirmation": {
                /**
                 *  send bid > "commit"
                 */
                changes += 1;
                break;
            }
            case "proposed": {
                /**
                 *  send bid > "waitingForManager"
                 */
                Double ownScore = calculateUtility(currentDecisionTask);
                //todo: eigene utillity speichern
                // send bid
                // ursprung des proposed job bestimmen
                ArrayList<String> values = new ArrayList<>();

                values.add(currentDecisionTask.getJobID());
                values.add("#");
                values.add(String.valueOf(ownScore));

                //zb. values = jobid # score
                testTrikeToTrikeService(currentDecisionTask.getOrigin(), "Propose", "Propose", values);
                currentDecisionTask.setStatus("waitingForManager");

                changes += 1;
                break;
            }
            case "notAssigned": {
                //todo in erledigt verschieben
                FinishedDecisionTaskList.add(currentDecisionTask);
                decisionTaskList.remove(index);
                break;
            }
            case "waitingForConfirmations": {
                //todo: test timeout here
                // just a temporary solution for the paper
                // workaround for the not workign confirmation
                currentDecisionTask.setStatus("delegated"); //todo: not shure if this is working corect
                FinishedDecisionTaskList.add(currentDecisionTask); //todo: not shure if this is working corect
                decisionTaskList.remove(index);//todo: not shure if this is working corect
                break;
            }
            case "waitingForManager": {
                //todo: test timeout here
                break;
            }
            case "committed":{
                System.out.println("should not exist: " + decisionTaskList.get(index).getStatus());
                //decisionTaskList.remove(0);
                break;
            }
            default: {
                //System.out.println("invalid status: " + decisionTaskList.get(position).getStatus());
                break;
            }
        }
        return changes;
    }


    public Double timeInSeconds(LocalDateTime time) {
        // Option 1: If the difference is greater than 300 seconds (5 minutes OR 300 seconds or 300000 millisec), then customer missed, -oemer

        double vaTimeSec = time.atZone(ZoneId.systemDefault()).toEpochSecond();
        return vaTimeSec;
    }

    //test if there is at least one trip anywhere
    public Trip getLastTripInPipeline(){
        Trip lastTrip = null;
        if (tripList.size()>0){
            lastTrip = tripList.get(tripList.size()-1);
        }
        else if (currentTrip.size()>0){
            lastTrip = currentTrip.get(currentTrip.size()-1);

        }
        else{
            System.out.println("ERROR: getLastTripInPipeline() no trips available!");
        }
        return lastTrip;
    }



    /** Utillity Function
     * should be switchable between a regular and a learning attempt
     * todo: assumption bookingtime = vatime
     * todo: fortschritt von currenttrip berücksichtigen!
     * @return
     */
    Double calculateUtility(DecisionTask newTask){
        Double utillityScore = 0.0;


        if (chargingTripAvailable.equals("0")) {


            newTask.getStartPositionFromJob();
            newTask.getEndPositionFromJob();
            newTask.getVATimeFromJob();

            Double a = 1.0 / 3.0;
            Double b = 1.0 / 3.0;
            Double c = 1.0 / 3.0;

            Double uPunctuality = null;
            Double uBattery = null;
            Double uDistance = null;

            //###########################################################
            // punctuallity
            // arrival delay to arrive at the start position when started from the agentLocation
            //todo: number of comitted trips TIP über alle berechnen erwartete ankunft bei aktuellem bestimmen, dann delay bewerten ohne ladefahrten
            Double vaTimeFirstTrip = null;
            //when there is no Trip before calculate the delay when started at the Agent Location
            if (currentTrip.size() == 0 && tripList.size() == 0) {
                //agentLocation
                Double distanceToStart = Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
                //Double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
                Double timeToNewTask = ((distanceToStart/1000) / DRIVING_SPEED)*60*60; //in this case equals the delay as vatiem is bookingtime
                // transforms the delay in seconds into as score beween 0 and 100 based of the max allowed delay of 900s
                if (timeToNewTask<THETA){
                    uPunctuality = 100.0;
                }
                else if (THETA<= timeToNewTask && timeToNewTask<=2*THETA){
                    uPunctuality = 100.0 - ((100.0 * timeToNewTask - THETA)/THETA);
                }
                else{
                    uPunctuality = 0.0;
                }

                //uPunctuality = Math.min(100.0, (100.0 - (((Math.min(THETA, timeToNewTask) - 0.0) / (THETA - 0.0)) * 100.0)));
            }
            else {
                Double totalDistance_TIP = 0.0;
                //todo: get va time of first job here or in an else case
                if (currentTrip.size() == 1) { //distances driven from the agent location to the start of the current trip and to its end
                    totalDistance_TIP += Location.distanceBetween(agentLocation, currentTrip.get(0).getStartPosition());
                    if (currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                        vaTimeFirstTrip = timeInSeconds(currentTrip.get(0).getVATime());
                        totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), currentTrip.get(0).getEndPosition());
                    }
                }
                //  distance driven at tripList
                if (tripList.size() > 0) {
                    if (currentTrip.size() > 0) { //journey to the first entry in the tripList from a currentTrip
                        if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getEndPosition(), tripList.get(0).getStartPosition());
                        } else { // trips with only a start position
                            totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), tripList.get(0).getStartPosition());
                        }
                    } else { //journey to the first entry in the tripList from the agentLocation
                        vaTimeFirstTrip = timeInSeconds(tripList.get(0).getVATime()); //fist VATime when there was no CurrentTrip
                        totalDistance_TIP += Location.distanceBetween(agentLocation, tripList.get(0).getStartPosition());
                    }
                    // distance driven at TripList.get(0)
                    if (tripList.get(0).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(tripList.get(0).getStartPosition(), tripList.get(0).getEndPosition());
                    }
                } else {
                    // do nothing as all other Trips with only a startPosition will not contain any other movements;
                }

                // interates through all other Trips inside TripList
                if (tripList.size() > 1){ //added to avoid crashes
                    for (int i = 1; i < tripList.size(); i++) {
                        if (tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getEndPosition(), tripList.get(i).getStartPosition()); //triplist or currenttrip
                        } else { // Trips with only a startPosition
                            totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getStartPosition(), tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                        }
                        if (tripList.get(i).getTripType().equals("CustomerTrip")) { //triplist or currenttrip
                            totalDistance_TIP += Location.distanceBetween(tripList.get(i).getStartPosition(), tripList.get(i).getEndPosition());
                        }
                    }
                }
                //todo: drives to the start of the job that has to be evaluated
                if (getLastTripInPipeline().getTripType().equals("CustomerTrip")) {
                    totalDistance_TIP += Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
                }
                else {
                    totalDistance_TIP += Location.distanceBetween(getLastTripInPipeline().getStartPosition(), newTask.getStartPositionFromJob());
                }


                Double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
                Double timeToNewTask = ((totalDistance_TIP/1000) / DRIVING_SPEED)*60*60;
                Double arrivalAtNewtask = vaTimeFirstTrip + timeToNewTask;

                Double delayArrvialNewTask = Math.max((arrivalAtNewtask - vATimeNewTask), timeToNewTask);
                System.out.println("vATimeNewTask: " + vATimeNewTask );
                System.out.println("timeToNewTask: " + timeToNewTask );
                System.out.println("arrivalAtNewtask: " + arrivalAtNewtask );
                System.out.println("delayArrvialNewTask: " + delayArrvialNewTask );

                if (delayArrvialNewTask<THETA){
                    uPunctuality = 100.0;
                }
                else if (THETA<= delayArrvialNewTask && delayArrvialNewTask <=2*THETA){
                    uPunctuality = 100.0 - ((100.0 * delayArrvialNewTask - THETA)/THETA);
                }
                else{
                    uPunctuality = 0.0;
                }

                //uPunctuality = Math.min(100.0, (100.0 - (((Math.min(THETA, delayArrvialNewTask) - 0.0) / (THETA - 0.0)) * 100.0)));



            }
            //when there a trips iterate through all, starting at the va time of the first trip estimate your delay when arriving at the start location of
            // the Job that has to be evaluated


            //###########################################################
            // Battery
            //todo: battery from Ömer needed
            // differ between trips with and without customer???
            Double currentBatteryLevel = trikeBattery.getMyChargestate(); //todo: use real battery
            Double estBatteryLevelAfter_TIP = trikeBattery.getMyChargestate();
            Double estDistance = 0.0;
            Double estEnergyConsumption = 0.0;
            Double estEnergyConsumption_TIP = 0.0;
            Double totalDistance_TIP = 0.0;
            Double negativeInfinity = Double.NEGATIVE_INFINITY;
            Double bFactor = null;
            //todo ennergieverbrauch für zu evuluierenden job bestimmen

            //calculation of the estimatedEnergyConsumtion (of formertrips)


            if (currentTrip.size() == 1) { //battery relavant distance driven at currentTrip
                //todo: fortschritt von currenttrip berücksichtigen
                totalDistance_TIP += Location.distanceBetween(agentLocation, currentTrip.get(0).getStartPosition());
                if (currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                    totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), currentTrip.get(0).getEndPosition());
                }
                if (currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                    totalDistance_TIP = 0.0; //reset the distance until now because only the distance after a chargingTrip influences the battery
                }
            }
            // battery relavant distance driven at tripList
            if (tripList.size() > 0) {
                if (currentTrip.size() > 0) { //journey to the first entry in the tripList from a currentTrip
                    if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getEndPosition(), tripList.get(0).getStartPosition());
                    } else { // trips with only a start position
                        totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), tripList.get(0).getStartPosition());
                    }
                } else { //journey to the first entry in the tripList from the agentLocation
                    totalDistance_TIP += Location.distanceBetween(agentLocation, tripList.get(0).getStartPosition());
                }
                // distance driven at TripList.get(0)
                if (tripList.get(0).getTripType().equals("CustomerTrip")) {
                    totalDistance_TIP += Location.distanceBetween(tripList.get(0).getStartPosition(), tripList.get(0).getEndPosition());
                }
                if (tripList.get(0).getTripType().equals("ChargingTrip")) {
                    totalDistance_TIP = 0.0;
                } else {
                    // do nothing as all other Trips with only a startPosition will not contain any other movements;
                }


                //todo: fahrt zum nächjsten start fehlt +-1 bei i???
                // interates through all other Trips inside TripList
                if (tripList.size() > 1){ //added to avoid crashes
                    for (int i = 1; i < tripList.size(); i++) {
                        if (tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getEndPosition(), tripList.get(i).getStartPosition()); //triplist or currenttrip
                        } else { // Trips with only a startPosition
                            totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getStartPosition(), tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                        }
                        if (tripList.get(i).getTripType().equals("CustomerTrip")) { //triplist or currenttrip
                            totalDistance_TIP += Location.distanceBetween(tripList.get(i).getStartPosition(), tripList.get(i).getEndPosition());
                        }
                    }
                }
            }
            //todo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RICHTIGE WERTE ZUGREIFEN
            estEnergyConsumption_TIP = trikeBattery.SimulateDischarge(totalDistance_TIP * DISTANCE_FACTOR);//*2 because it would be critical to underestimate the distance
            estBatteryLevelAfter_TIP = currentBatteryLevel - estEnergyConsumption_TIP;

            //calculate teh estimated energy consumption of the new job


            //Distance from the agent location
            if (currentTrip.size() == 0 && tripList.size() == 0){
                estDistance += Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
            }
            //Distance from the Last Trip in Pipe
            else{
                if (getLastTripInPipeline().getTripType().equals("CustomerTrip")){
                    estDistance += Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
                }
                else{
                    estDistance += Location.distanceBetween(getLastTripInPipeline().getStartPosition(), newTask.getStartPositionFromJob());
                }
            }
            estDistance += Location.distanceBetween(newTask.getStartPositionFromJob(), newTask.getEndPositionFromJob());

            estEnergyConsumption = trikeBattery.SimulateDischarge(estDistance * DISTANCE_FACTOR);

            Double estBatterylevelTotal = estBatteryLevelAfter_TIP - estEnergyConsumption;


            //###########################################################
            // calculation of uBattery
            if (estBatterylevelTotal < 0.0) { //todo: estEnergyConsumption FEHLT!
                uBattery = negativeInfinity;
            } else {
                if (estBatterylevelTotal > 0.8) {
                    bFactor = 1.0;
                } else if (estBatterylevelTotal >= 0.3) {
                    bFactor = 0.75;
                } else if (estBatterylevelTotal < 0.3) {
                    bFactor = 0.1;
                }
                // ???? batteryLevelAfterTrips or 100?
                uBattery = (bFactor * estBatterylevelTotal) * 100;

            }
            //###########################################################
            //Distance
            Double dmax = 3000.0;
            Double distanceToStart;

            if (tripList.size() == 0 && currentTrip.size() == 0) {
                distanceToStart = Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
            } else {
                if (getLastTripInPipeline().getTripType().equals("CustomerTrip")) {
                    distanceToStart = Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
                } else {
                    distanceToStart = Location.distanceBetween(getLastTripInPipeline().getStartPosition(), newTask.getStartPositionFromJob());
                }
            }
            uDistance = Math.max(0, (100-distanceToStart / dmax));
            //uDistance = Math.max(0, Math.min(100, (100.0 - ((distanceToStart / dmax) * 100.0))));


            //###########################################################


            // calculate the total score

            utillityScore = Math.max(0.0, (a * uPunctuality + b * uBattery + c * uDistance));
        }
        System.out.println("agentID: " + agentID + "utillity: " + utillityScore);
        return utillityScore;
    }

    //estimates the batteryLevel after all Trips. Calculations a based on aerial line x1.5
    public Double estimateBatteryAfterTIP(){
        Double batteryChargeAfterTIP = trikeBattery.getMyChargestate();
        Double totalDistance_TIP = 0.0;
        if (currentTrip.size() == 1) { //battery relavant distance driven at currentTrip
            //todo: fortschritt von currenttrip berücksichtigen
            totalDistance_TIP += Location.distanceBetween(agentLocation, currentTrip.get(0).getStartPosition());
            if (currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), currentTrip.get(0).getEndPosition());
            }
            if (currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                totalDistance_TIP = 0.0; //reset the distance until now because only the distance after a chargingTrip influences the battery
            }
        }
        // battery relavant distance driven at tripList
        if (tripList.size() > 0) {
            if (currentTrip.size() > 0) { //journey to the first entry in the tripList from a currentTrip
                if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                    totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getEndPosition(), tripList.get(0).getStartPosition());
                } else { // trips with only a start position
                    totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), tripList.get(0).getStartPosition());
                }
            } else { //journey to the first entry in the tripList from the agentLocation
                totalDistance_TIP += Location.distanceBetween(agentLocation, tripList.get(0).getStartPosition());
            }
            // distance driven at TripList.get(0)
            if (tripList.get(0).getTripType().equals("CustomerTrip")) {
                totalDistance_TIP += Location.distanceBetween(tripList.get(0).getStartPosition(), tripList.get(0).getEndPosition());
            }
            if (tripList.get(0).getTripType().equals("ChargingTrip")) {
                totalDistance_TIP = 0.0;
            } else {
                // do nothing as all other Trips with only a startPosition will not contain any other movements;
            }

            //todo: fahrt zum nächjsten start fehlt +-1 bei i???
            // interates through all other Trips inside TripList
            if (tripList.size() > 1){ //added to avoid crashes
                for (int i = 1; i < tripList.size(); i++) {
                    if (tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getEndPosition(), tripList.get(i).getStartPosition()); //triplist or currenttrip
                    } else { // Trips with only a startPosition
                        totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getStartPosition(), tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                    }
                    if (tripList.get(i).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(tripList.get(i).getStartPosition(), tripList.get(i).getEndPosition());
                    }
                }
            }
        }
        Double estEnergyConsumption_TIP = trikeBattery.SimulateDischarge((totalDistance_TIP * DISTANCE_FACTOR));
        batteryChargeAfterTIP = batteryChargeAfterTIP - estEnergyConsumption_TIP;

        estimateBatteryAfterTIP.set(0, batteryChargeAfterTIP);
        return batteryChargeAfterTIP;
    }


    /**
     *  MaintainTripService former SendDrivetoTooutAdc
     *  desired behavior:
     *  start: when new trip is generated
     */
    @Goal(recur = true, recurdelay = 300)
    class MaintainTripService {

        @GoalMaintainCondition
        boolean sentToMATSIM() {
            return !(isMatsimFree && canExecute);
        }
    }

    /**
     * DoNextTrip() former PlansendDriveTotoOutAdc()
     */
    ///**
    @Plan(trigger = @Trigger(goals = MaintainTripService.class))
    public void DoNextTrip() {
        ExecuteTrips();
    }



    //#######################################################################
    //Goals and Plans : After the agentID is assigned to the Trike Agent,
    // Trike Agent should prepare everything for the synchronization process
    //#######################################################################

    @Goal(recur = true, recurdelay = 3000)
    class ReactoAgentIDAdded {
        public ReactoAgentIDAdded() {
        }
    }

    @Plan(trigger = @Trigger(goals = ReactoAgentIDAdded.class))
    private void ReacttoAgentIDAdded()
    {
        if (agentID != null) // only react if the agentID exists
        {
            if (SimIDMapper.NumberSimInputAssignedID.size() == JadexModel.SimSensoryInputBrokernumber) // to make sure all SimInputBroker also receives its ID so vehicle agent could choose one SimInputBroker ID to register
                if (!sent) { // to make sure the following part only executed once
                    sent = true;
                    System.out.println("The agentid assigned for this vehicle agent is " + this.agentID);
                    // setTag for itself to receive direct communication from SimSensoryInputBroker when service INotifyService is used.
                    IServiceIdentifier sid = ((IService) agent.getProvidedService(INotifyService.class)).getServiceId();
                    agent.setTags(sid, "" + agentID);
                    //choosing one SimSensoryInputBroker to receive data from MATSIM
                    currentSimInputBroker = getRandomSimInputBroker();

                    // setTag for itself to receive direct communication from TripRequestControlAgent when service IsendTripService is used.
                    IServiceIdentifier sid2 = ((IService) agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
                    agent.setTags(sid2, "" + agentID);

                    //communicate with SimSensoryInputBroker when knowing the serviceTag of the SimSensoryInputBroker.
                    ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
                    query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
                    query.setServiceTags("" + currentSimInputBroker); // choose to communicate with the SimSensoryInputBroker that it registered befre
                    Collection<INotifyService2> service = agent.getLocalServices(query);
                    for (INotifyService2 cs : service) {
                        cs.NotifyotherAgent(agentID); // write the agentID into the list of the SimSensoryInputBroker that it chose before
                    }
                    System.out.println("agent "+ this.agentID +"  registers at " + currentSimInputBroker);
                    // Notify TripRequestControlAgent and JADEXModel
                    TrikeMain.TrikeAgentNumber = TrikeMain.TrikeAgentNumber+1;
                    JadexModel.flagMessage2();
                    //action perceive is sent to matsim only once in the initiation phase to register to receive events
                    SendPerceivetoAdc();

                    agentLocation = Cells.trikeRegisterLocations.get(agentID);
                    sendAreaAgentUpdate("register");

                    // Print the initial location for verification
                    System.out.println("Agent " + agentID + " initial location: " + agentLocation);

                    if(FIREBASE_ENABLED){
                        //update the location of the agent
                        FirebaseHandler.updateAgentLocation(agentID, agentLocation);
                    }

                    //csvLogger csvLogger = new csvLogger(agentID);
                    csvLogger csvLogger = new csvLogger(agentID, CNP_ACTIVE, THETA, ALLOW_CUSTOMER_MISS, CHARGING_THRESHOLD, commitThreshold, DISTANCE_FACTOR);

                }
        }
    }

    //#######################################################################
    //Goals and Plans : to print out something when the data from MATSIM is
    //written to its belief base by the SimSensoryInputBroker
    //#######################################################################

    @Goal(recur = true,recurdelay = 300)
    class PerformSIMReceive {
    }

    @Plan(trigger = @Trigger(goals = PerformSIMReceive.class))
    public void SensoryUpdate() {
        currentTripStatus();
        if(actionContentRingBuffer.isEmpty()) return;
        if (isMatsimFree && !currentTrip.isEmpty()) {
            ActionContent actionContent = actionContentRingBuffer.read();
            if (actionContent.getAction_type().equals("drive_to") && actionContent.getState() == ActionContent.State.PASSED) {
                System.out.println("Agent " + agentID + " finished with the previous trip and now can take the next trip");
                System.out.println("AgentID: " + agentID + actionContent.getParameters()[0]);
                double metersDriven = Double.parseDouble((String) actionContent.getParameters()[1]);
                updateBeliefAfterAction(metersDriven);
                canExecute = true;
                updateAtInputBroker();
            }
        }
        currentTripStatus();
    }

    /**
     * for the sny of the cycle
     */
    void updateAtInputBroker(){
        ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
        query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
        query.setServiceTags("" + currentSimInputBroker);
        Collection<INotifyService2> service = agent.getLocalServices(query);
        for (Iterator<INotifyService2> iteration = service.iterator(); iteration.hasNext(); ) {
            INotifyService2 cs = iteration.next();
            cs.removeTrikeAgentfromActiveList(agentID);
            System.out.println(" Newly active Agent " + agentID + "notifies" + currentSimInputBroker + " that it finished deliberating");
        }
    }


    void prepareLog(Trip trip, String batteryBefore, String batteryAfter, String arrivedAtLocation, String distance){
        String tripID = trip.getTripID();
        String tripType = trip.getTripType();
        String driveOperationNumber = "1";
        String origin = "";
        if (trip.getProgress().equals("AtEndLocation")){
            driveOperationNumber = "2";
        }
        String arrivalTime = "0.0"; //when it was not a CustomerTrip
        if (trip.getTripType().equals("CustomerTrip")){
            arrivalTime = Double.toString(ArrivalTime(trip.getVATime()));
            origin = "trike:" + trip.getDecisionTaskD().getOrigin();
        }
        csvLogger.addLog(agentID, CNP_ACTIVE, THETA, ALLOW_CUSTOMER_MISS, CHARGING_THRESHOLD, commitThreshold, DISTANCE_FACTOR, "trike:" + agentID, tripID, driveOperationNumber, tripType, batteryBefore, batteryAfter, arrivedAtLocation, distance, arrivalTime, origin);
    }



    // After a succefull action in MATSIm: Updates the progreess of the current Trip and the Agent location
    //todo: better get the location from MATSim
    void updateBeliefAfterAction(double metersDriven) {
        Trip CurrentTripUpdate = currentTrip.get(0);
        //double metersDriven = 100.0;
        //Transport ohne Kunde
        String arrivedAtLocation = "true";

        if (CurrentTripUpdate.getProgress().equals("DriveToStart")) {
            updateCurrentTripProgress("AtStartLocation");
            agentLocation = CurrentTripUpdate.getStartPosition();
            String batteryBefore = Double.toString(trikeBattery.getMyChargestate()); //todo: vorher schieben
            trikeBattery.discharge(metersDriven, 0);
            String batteryAfter = Double.toString(trikeBattery.getMyChargestate());
            //String arrivedAtLocation = "true";
            if (trikeBattery.getMyChargestate() < 0.0){
                arrivedAtLocation = "false";
                updateCurrentTripProgress("Failed");

            }
            String distance = Double.toString(metersDriven);
            prepareLog(CurrentTripUpdate, batteryBefore, batteryAfter, arrivedAtLocation, distance);

            if (arrivedAtLocation.equals("false")){
                currentTrip.remove(0);
                terminateTripList();
            }
        }


        //Transport mit Kunde
        if (CurrentTripUpdate.getProgress().equals("DriveToEnd")){
            updateCurrentTripProgress("AtEndLocation");
            agentLocation = CurrentTripUpdate.getEndPosition();
            String batteryBefore = Double.toString(trikeBattery.getMyChargestate()); //todo: vorher schieben
            trikeBattery.discharge(metersDriven, 1);
            String batteryAfter = Double.toString(trikeBattery.getMyChargestate());
            if(FIREBASE_ENABLED){
                FirebaseHandler.removeChildEventListener("trips/"+currentTrip.get(0).getTripID(), listenerHashMap.get(currentTrip.get(0).getTripID()));
            }
            //String arrivedAtLocation = "true";
            if (trikeBattery.getMyChargestate() < 0.0){
                arrivedAtLocation = "false";
                updateCurrentTripProgress("Failed");
            }
            String distance = Double.toString(metersDriven);
            prepareLog(CurrentTripUpdate, batteryBefore, batteryAfter, arrivedAtLocation, distance);

            if (arrivedAtLocation.equals("false")){
                currentTrip.remove(0);
                terminateTripList();
            }
        }

        if(FIREBASE_ENABLED){
            // Update Firebase with the current progress
            FirebaseHandler.sendTripProgress(CurrentTripUpdate.getTripID(), CurrentTripUpdate.getProgress());
            /**
             * TODO: @Mariam update firebase after every MATSim action: location of the agent
             */


            System.out.println("Neue Position:" + agentLocation);
            FirebaseHandler.updateAgentLocation(agentID, agentLocation);
        }




        System.out.println("Neue Position: " + agentLocation);
        sendAreaAgentUpdate("update");


        //todo: action und perceive trennen! aktuell beides in beiden listen! löschen so nicht konsistent!
        //TODO: @Mahkam send Updates to AreaAgent
        currentTripStatus();
    }

    //remove all Trips from tripList and currenTrip and write them with the logger
    public void terminateTripList(){
        if (currentTrip.size() > 1){
            prepareLog(currentTrip.get(0), "0.0", "0.0", "false", "0.0");
            currentTrip.get(0).setProgress("Failed");
            currentTrip.remove(0);



        }
        if (tripList.size() > 0){
            while (tripList.size() > 0) {
                prepareLog(tripList.get(0), "0.0", "0.0", "false", "0.0");
                tripList.get(0).setProgress("Failed");
                tripList.remove(0);
            }
        }
        trikeBattery.loadBattery();
        chargingTripAvailable = "0";

        System.out.println("AgentID: " + agentID + "ALL TRIPS TERMINATED");
    }

    public void AddAgentNametoAgentList()
    {
        SimIDMapper.TrikeAgentNameList.add(agent.getId().getName());
    }

    public void AddTriptoTripList(Trip Trip)
    {
        tripList.add(Trip);
    }

    public void AddTripIDTripList(String ID)
    {
        tripIDList.add(ID);
    }

    public void AddDecisionTask(DecisionTask decisionTask)
    {
        decisionTaskList.add(decisionTask);
    }

    public void setAgentID(String agentid) {
        agentID = agentid;
    }

    public String getAgentID() {
        System.out.println(agentID);

        return agentID;
    }



    //just for a test delete after
    public void setTestList(String TextMessage) {
        //TestList.add(TextMessage);
        System.out.println("Service: new Trip received " + TextMessage);
    }


    public String getRandomSimInputBroker() // choose random SimInputBroker to register in the begining
    {
        List<String> SimInputBrokerList = SimIDMapper.NumberSimInputAssignedID;
        Random rand = new Random();
        String randomSimInputBroker = SimInputBrokerList.get(rand.nextInt(SimInputBrokerList.size()));
        return randomSimInputBroker;
    }

    //#######################################################################
    //Methods uses for sending trip info to data container
    //#######################################################################

    void newCurrentTrip(){
        if(currentTrip.isEmpty() && !tripList.isEmpty()){
            currentTrip.add(tripList.remove(0));
        }
    }

    /** Updates the progress of the CurrentTrip
     *
     * @param newProgress
     */
    void updateCurrentTripProgress(String newProgress) {
        Trip CurrentTripUpdate = currentTrip.get(0);
        CurrentTripUpdate.setProgress(newProgress);
        currentTrip.set(0, CurrentTripUpdate);
        currentTripStatus();
    }

    void currentTripStatus() {
        if (currentTrip.size() > 0){
            System.out.println("\n currentTripStatus:");
            System.out.println("AgentID: " + agentID + " currentTripID: " + currentTrip.get(0).getTripID());
            System.out.println("AgentID: " + agentID + " currentTripType: " + currentTrip.get(0).getTripType());
            System.out.println("AgentID: " + agentID + " currentVaTime: " + currentTrip.get(0).getVATime());
            System.out.println("AgentID: " + agentID + " currentStartPosition: " + currentTrip.get(0).getStartPosition());
            System.out.println("AgentID: " + agentID + " currentEndPosition: " +currentTrip.get(0).getEndPosition());
            System.out.println("AgentID: " + agentID + " currentProgress: " + currentTrip.get(0).getProgress());
        }

    }

    void Status(){
        /*
        //if (agentID.equals("0")){
        System.out.println("AgentID: " + agentID + " activestatus: " + isMatsimFree);
        System.out.println("AgentID: " + agentID + " currentTrip.size: " + currentTrip.size());
        System.out.println("AgentID: " + agentID + " tripList.size: " + tripList.size());
        System.out.println("AgentID: " + agentID + " decisionTaskList.size: " + decisionTaskList.size());
        System.out.println("AgentID: " + agentID + " SimActionList: " + SimActionList.size());
        System.out.println("AgentID: " + agentID + " SimPerceptList: " + SimPerceptList.size());
        //for (ActionContent actionContent : SimActionList) {
        //System.out.println("AgentID: " + agentID + " actionType: "+ actionContent.getAction_type() + " actionState: " + actionContent.getState());
        //}
        for (int i=0; i<decisionTaskList.size(); i++){
            System.out.println("AgentID: " + agentID + " decisionTaskList status: " + decisionTaskList.get(i).getStatus());
        }


        currentTripStatus();
        //}

         */
    }

    public Double ArrivalTime(LocalDateTime vATime){
        long offset = (vATime
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        long vaTimeMilli = vATime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double curr = (JadexModel.simulationtime) * 1000;
        double diff = (curr - (vaTimeMilli - offset))/1000 ; //in seconds
        //Double arrivalTime;
        return diff;
    };



    public boolean customerMiss(Trip trip) {
        long offset = (trip.getVATime()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());



        // Option 1: If the difference is greater than 300 seconds (5 minutes OR 300 seconds or 300000 millisec), then customer missed, -oemer
        boolean isMissed = false;
        long vaTimeMilli = trip.getVATime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        //Double vaTimeSec = timeInSeconds(currentTrip.get(0).getVATime());
        double curr = (JadexModel.simulationtime) * 1000;
        double diff = curr - (vaTimeMilli - offset) ;
        if (diff > (THETA*1000) && ALLOW_CUSTOMER_MISS){
            return isMissed = true;
        }
        return isMissed;
    }
    /** old version
     public boolean customerMiss(Trip trip) {
     // Option 1: If the difference is greater than 300 seconds (5 minutes OR 300 seconds or 300000 millisec), then customer missed, -oemer
     boolean isMissed = false;
     double vaTimeMilli = trip.getVATime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
     double curr = JadexModel.simulationtime;
     double diff = (vaTimeMilli - (curr * 30000000));
     if (diff >= 1.6000000E13){
     return isMissed = true;
     }
     return isMissed;
     }
     **/

    public boolean customerMissProb(Trip trip) {
        // Option 2: If the difference is greater than 600 seconds (10 minutes OR 600 seconds or 600000 millisec), then customer probably missed, -oemer
        boolean isMissed = false;
        double vaTimeMilli = trip.getVATime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double curr = JadexModel.simulationtime;
        double diff = (vaTimeMilli - (curr * 100000000));
        if (diff >= 600000000) {
            double probability = 0.05 * vaTimeMilli;
            isMissed = new Random().nextDouble() < probability;
        }
        return isMissed;
    }

    /**
     *  handles the progress of the current Trip
     */
    public void ExecuteTrips() {
        newCurrentTrip();

        Trip current = null;
        if(!currentTrip.isEmpty()){
            current = currentTrip.get(0);
        }

        if (current != null) {
            currentTripStatus();
            switch (current.getProgress()) {
                case "NotStarted": {
                    canExecute = false;
                    isMatsimFree = false;
                    sendDriveTotoAdc();
                    updateCurrentTripProgress("DriveToStart");
                    break;
                }
                case "AtStartLocation": {
                    switch (current.getTripType()) {
                        case "ChargingTrip": {
                            trikeBattery.loadBattery();
                            updateCurrentTripProgress("Finished");
                            chargingTripAvailable = "0";
                            break;
                        }
                        case "CustomerTrip": {
                            if (customerMiss(current)) { // customer not there
                                updateCurrentTripProgress("Failed");
                            } else {
                                canExecute = false;
                                isMatsimFree = false;
                                sendDriveTotoAdc();
                                updateCurrentTripProgress("DriveToEnd");
                            }
                            break;
                        }
                        default: {
                            updateCurrentTripProgress("Finished");
                            break;
                        }
                    }
                    break;
                }
                case "AtEndLocation": {
                    updateCurrentTripProgress("Finished");
                    break;
                }
                case "Finished":
                case "Failed": {
                    currentTrip.remove(0);
                    currentTripStatus();
                    break;
                }
            }
            estimateBatteryAfterTIP();
        }
    }

    public void sendDriveTotoAdc()
    {
        Object[] Endparams = new Object[7];
        // needs to get seperate parameter for different types of trip
        if (currentTrip.get(0).getProgress().equals("NotStarted"))
        {
            Endparams[0] = Constants.DRIVETO;
            Endparams[1] = currentTrip.get(0).getStartPosition().getCoordinates();

        }
        if (currentTrip.get(0).getProgress().equals("AtStartLocation"))
        {
            Endparams[0] = Constants.DRIVETO;
            Endparams[1] = currentTrip.get(0).getEndPosition().getCoordinates();
        }
        Endparams[2] = JadexModel.simulationtime;
        Endparams[3] = Constants.EvacRoutingMode.carFreespeed;
        Endparams[4] = "EvacPlace";
        Endparams[5] = currentTrip.get(0).getTripID();
        //added oemer
        Endparams[6] = sumLinkLength;
        SimActuator.getEnvironmentActionInterface().packageAction(agentID, "drive_to", Endparams, null);
    }

    public void SendPerceivetoAdc() // needs to send in the begining to subscribe to events in MATSIM
    {
        Object[] params = new Object[8];
        params[0] = "blocked";
        params[1] = "congestion";
        params[2] = "arrived"; // five secs from now;
        params[3] = "departed";
        params[4] = "activity_started";
        params[5] = "activity_ended"; // add replan activity to mark location/time of replanning
        params[6] = "stuck";
        params[7] = "sum_link_length"; //added -oemer

        SimActuator.getEnvironmentActionInterface().packageAction(agentID, "perceive", params, "");
    }

    public double getDrivingDistanceTo(Location location) throws AgentNotFoundException { // EUclician Distanz
        double dist =
                (double)SimActuator.getQueryPerceptInterface().queryPercept(
                        String.valueOf(agentID),
                        Constants.REQUEST_DRIVING_DISTANCE_TO,
                        location.getCoordinates());
        return dist;
    }

    public  Location getCurrentLocation() throws AgentNotFoundException {
        Location CurrentLocation = (Location) SimActuator.getQueryPerceptInterface().queryPercept(String.valueOf(agentID), Constants.REQUEST_LOCATION, null);

        return CurrentLocation;
    }
    ///////////////////////////////////////////////////////
    //  updates locatedagentlist of the area agent


    //  example of trike to trike communication
    void sendMessageToTrike(String receiverID, String comAct, String action, ArrayList<String> values){
        //message creation
        //ArrayList<String> values = new ArrayList<>();
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", ""+agentID, receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.trikeReceiveTrikeMessage(testMessage.serialize());
    }


    //  example of trike to trike communic ation
    public void testTrikeToTrikeService(String receiverID, String comAct, String action, ArrayList<String> values){
        //message creation
        //ArrayList<String> values = new ArrayList<>();
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", agentID,""+receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.trikeReceiveTrikeMessage(testMessage.serialize());
    }

    //
    public void sendMessage(String receiverID, String comAct, String action, ArrayList<String> values){
        //todo adapt for multiple area agents
        //todo use unique ids
        //message creation

        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", ""+agentID, receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.receiveMessage(testMessage.serialize());

    }


    void sendAreaAgentUpdate(String action){
        //location
        ArrayList<String> values = new ArrayList<>();
        values.add(Double.toString(agentLocation.getX()));
        values.add(Double.toString(agentLocation.getY()));

        //update the cell based on location
        String foundKey = Cells.findKey(agentLocation);
        int resolution = Cells.getCellResolution(foundKey);
        newCellAddress = Cells.locationToCellAddress(agentLocation, resolution);

        //  init register of trikes
        if (action.equals("register")){
            oldCellAddress = newCellAddress;
        }

        // if the cell address has changed, change the area and leave the method
        if (!oldCellAddress.equals(newCellAddress)){
            String originArea = Cells.cellAgentMap.get(oldCellAddress);
            String newArea = Cells.cellAgentMap.get(newCellAddress);
            changeArea(originArea, newArea);
            oldCellAddress = newCellAddress;
            return;
        }

        //  get target AreaAgent tag based on the cell address
        String areaAgentTag = Cells.cellAgentMap.get(newCellAddress);

        //  update/register message
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("0", agentID, areaAgentTag, "inform", JadexModel.simulationtime,  messageContent);

        //query assigning
        IAreaTrikeService service = messageToService(agent, testMessage);
        //calls updateAreaAgent of AreaAgentService class
        service.areaReceiveUpdate(testMessage.serialize());

    }

    public void test(){
        ArrayList<String> values = new ArrayList<>();
        sendMessage("area:0", "request", "callForNeighbours", values);
        //sendMessage("area:0", "inform", "update");

    }

    //Battery -oemer
    public void setMyLocation(Location location) {
    }

    public boolean isDaytime()
    {
        return this.daytime;
    }

    /**
     * Set the daytime of this Vision.
     * @param daytime the value to be set
     */
    public void setDaytime(boolean daytime)
    {
        this.daytime = daytime;

    }

    public void print(String str){
        System.out.println(agentID + ": " + str);
    }

/*
    @Goal(recur = true, recurdelay = 100)
    class LogSimActionList{
        @GoalMaintainCondition
        public boolean isEmpty(){
            return SimActionList.isEmpty();
        }

    }

    @Plan(trigger = @Trigger(goals = LogSimActionList.class))
    public void logSimActionList() throws IOException {
        logger = new MyLogger(agentID + "-Action.txt", MyLogger.Status.INFO);
        if(currentTrip.isEmpty()){
            logger.info("Current Trip: []");
        }else{
            logger.info("Current Trip: ");
            currentTrip.forEach((trip) -> logger.info(trip.tripID));
        }
        logger.newLine();
        if(tripList.isEmpty()){
            logger.info("Trips: []");
        }else{
            logger.info("Trips: ");
            tripList.forEach((trip) -> logger.info(trip.tripID));
        }
        logger.newLine();
        if(SimPerceptList.isEmpty()){
            logger.info("SimActionList: []");
        }else{
            logger.info("SimActionList: ");
            SimActionList.forEach((simAction) -> logger.info(simAction.toString()));
        }
        logger.newLine();
        logger.info("#########################################################");
        logger.close();
    }

    @Goal(recur = true, recurdelay = 100)
    class LogSimPerceptList{
        @GoalMaintainCondition
        public boolean isEmpty(){
            return SimPerceptList.isEmpty();
        }

    }

    @Plan(trigger = @Trigger(goals = LogSimPerceptList.class))
    public void logSimPerceptList() throws IOException {
        logger = new MyLogger(agentID + "-Percept.txt", MyLogger.Status.INFO);
        if(currentTrip.isEmpty()){
            logger.info("Current Trip: []");
        }else{
            logger.info("Current Trip: ");
            currentTrip.forEach((trip) -> logger.info(trip.tripID));
        }
        logger.newLine();
        if(tripList.isEmpty()){
            logger.info("Trips: []");
        }else{
            logger.info("Trips: ");
            tripList.forEach((trip) -> logger.info(trip.tripID));
        }
        logger.newLine();
        if(SimPerceptList.isEmpty()){
            logger.info("SimPerceptList: []");
        }else{
            logger.info("SimPerceptList: ");
            SimPerceptList.forEach((simAction) -> logger.info(simAction.toString()));
        }
        logger.newLine();
        logger.info("#########################################################");
        logger.close();
    }

    */

    //  send message to AreaAgents to deregister from old and register to new
    private void changeArea(String originArea, String newArea) {
        //deregister from old
        MessageContent messageContent = new MessageContent("deregister", null);
        Message deregisterMessage = new Message("0", agentID, originArea, "inform", JadexModel.simulationtime, messageContent);

        //query assigning
        IAreaTrikeService service = messageToService(agent, deregisterMessage);

        //calls updateAreaAgent of AreaAgentService class
        service.areaReceiveUpdate(deregisterMessage.serialize());

        //register to new
        messageContent = new MessageContent("register", null);
        Message registerMessage = new Message("0", agentID, newArea, "inform", JadexModel.simulationtime, messageContent);

        //query assigning
        service = messageToService(agent, registerMessage);

        //calls updateAreaAgent of AreaAgentService class
        service.areaReceiveUpdate(registerMessage.serialize());
    }
    private void configure(Element classElement) {
        this.FIREBASE_ENABLED = Boolean.parseBoolean(getClassField(classElement, "FIREBASE_ENABLED"));
        this.chargingTripAvailable = getClassField(classElement, "chargingTripAvailable");
        this.commitThreshold = Double.parseDouble(getClassField(classElement, "commitThreshold"));
        this.DRIVING_SPEED = Double.parseDouble(getClassField(classElement, "DRIVING_SPEED"));
        this.CNP_ACTIVE = Boolean.parseBoolean(getClassField(classElement, "CNP_ACTIVE"));
        this.THETA = Double.parseDouble(getClassField(classElement, "THETA"));
        this.ALLOW_CUSTOMER_MISS = Boolean.parseBoolean(getClassField(classElement, "ALLOW_CUSTOMER_MISS"));
        this.DISTANCE_FACTOR = Double.parseDouble(getClassField(classElement, "DISTANCE_FACTOR"));
        this.CHARGING_THRESHOLD = Double.parseDouble(getClassField(classElement, "CHARGING_THRESHOLD"));
    }
}