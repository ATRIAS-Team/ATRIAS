package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.matsim.EvacAgentTracker;
import io.github.agentsoz.ees.matsim.EvacAgentTracker.VehicleTrackingData;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.IPlan;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.jdeqsim.Vehicle;

import java.util.List;

public class BatteryModel {

    @Belief
    protected double my_numberofcharges = 200;
    @Belief
    protected double my_batteryhealth = 1.0 - 0.00025 * my_numberofcharges;
    @Belief
    protected double my_speed;
    @Belief
    protected boolean my_autopilot;
    @Belief
    //protected double my_chargestate = 0.21;
    protected double my_chargestate = 1.0;
    @Belief
    protected boolean daytime;
    @Belief
    public List<Location> chargingStation;
    @Belief
    private static Location agentLocation; // position of the agent

    @PlanCapability
    protected static TrikeAgent capa;
    @PlanAPI
    protected static IPlan rplan;
    @PlanReason
    protected static TrikeAgent.AchieveMoveTo goal;



    //Methode 2 added -oemer

    /*
     Charge als methode mit einklang mit Simulationszeit
	- Charging station als Liste in den Agenten oder eine
	eigene Klasse oder als Agent definieren
	- List<String> chargingstation
    Trike Agent.java: z. 713 Atchargingstation
    */


    private static void updateChargingProgress(TrikeAgent agent) {

        double chargingRate = 0.001;
        double newChargeState = agent.getMyChargestate() + chargingRate * JadexModel.simulationtime;
        agent.setMyChargestate(Math.min(newChargeState, 1.0));
    }


    public static void loadBattery(TrikeAgent agentapi, IPlan planapi, double simulationTime)
    {
        //Hier sucht sich der Trike Agent eine Station raus und fährt mit AchieveMoveTo zur Station
        //Das soll als Fahrauftrag erledigt werden in ATRIAS

        //commented out -oemer
        //   Chargingstation station = ((io.github.agentsoz.ees.jadexextension.masterthesis.trike.TrikeAgent.QueryChargingStation)planapi.dispatchSubgoal(agentapi.new QueryChargingStation()).get()).getStation();
        //   planapi.dispatchSubgoal(agentapi.new AchieveMoveTo(station.getLocation())).get();


            double charge = agentapi.getMyChargestate();
            double batteryhealth = agentapi.getMyBatteryHealth();
            double numberofcharges = agentapi.getMyNumberOfCharges();


            //TODO: Location of trike agent and location of Charging station
            // while (charge<1 && agentapi.getLocation().getDistance(station.getLocation())<0.01)

            // create a new ChargingStation  -oemer
            ChargingStation station = new ChargingStation();
            while (charge<1 && agentapi.getLocation().getDistance(station.getLocation())<0.01)
            {
                // Daytime
                if (agentapi.daytime)
                {
                    charge = Math.min(charge + 0.01, 1.0);

                    if (charge>0.99)
                    {
                        numberofcharges = numberofcharges + 1;
                    }
                }

                // Nighttime
                else
                {
                    charge = Math.min(charge + 0.005, 1.0);

                    if (charge>0.995)
                    {
                        numberofcharges = numberofcharges + 0.5;
                    }
                }

                simulationTime = JadexModel.simulationtime;
                updateChargingProgress(agentapi);

                agentapi.setMyChargestate(charge);
                planapi.waitFor(100).get();
                agentapi.setMyNumberOfCharges(numberofcharges);
                batteryhealth = 1.0 - 0.00025 * numberofcharges;
                agentapi.setMyBatteryHealth(batteryhealth);

                //create a new charging trip -oemer Frage : Soll diser Ladetrip bevor dem Aufladevorgang oder danach erstellt werden?
                Location LocationCh= new Location("", 288654.693529, 5286721.094209);
                Trip chargingTrip = new Trip("CH01", "ChargingTrip", LocationCh, "NotStarted");

            }

    }

    @PlanBody
    protected static IFuture<Void> moveToTarget() {
        final Future<Void> ret = new Future<Void>();

        Location target = goal.getLocation();
        Location myloc = agentLocation; //TODO: Location of the TrikeAgent

        if (!myloc.isNear(target)) { //comparison to other Location
            oneStepToTarget().addResultListener(new DelegationResultListener<Void>(ret) {
                public void customResultAvailable(Void result) {
                    moveToTarget().addResultListener(new DelegationResultListener<Void>(ret));
                }
            });
        } else {
            ret.setResult(null);
        }

        return ret;
    }

    @PlanBody
    protected static IFuture<Void> oneStepToTarget() {
        final Future<Void> ret = new Future<Void>();

        //Here the distance is calculated between 2 points by using a L1 norm
        Location target = goal.getLocation();
        Location myloc = agentLocation;


        double speed = capa.getMySpeed();
        boolean autopilot = capa.getMyAutopilot();
        double charge = capa.getMyChargestate();
        double batteryhealth = capa.getMyBatteryHealth();
        int carriedcustomer = capa.getCarriedCustomer();


        if (autopilot) {
            // In autopilot the speed is low.
            speed = 2.0;
            //changed from list to int -oemer
            if (carriedcustomer == 0) {
                // During an empty trip there is less weight to be moved by the trike agent.
                charge = charge - speed * 0.0002 * (1 / batteryhealth);
            } else {
                // During a customer trip the trike agent has to move weight of the customer.
                charge = charge - speed * 0.0008 * (1 / batteryhealth);
            }

        } else {
            // If the customer decides to drive himself, the speed is higher.
            speed = 4.0;
            charge = charge - speed * 0.0004 * (1 / batteryhealth);
        }

        capa.setMySpeed(speed);
        capa.setMyChargestate(Double.valueOf(charge));

        double d = myloc.getDistance(target);
        double r = speed * 0.004;//(newtime-time);
        double dx = target.getX() - myloc.getX();
        double dy = target.getY() - myloc.getY();

        double rx = r < d ? r * dx / d : dx;
        double ry = r < d ? r * dy / d : dy;
        //System.out.println("mypos: "+(myloc.getX()+rx)+" "+(myloc.getY()+ry)+" "+target);
        capa.setMyLocation(new Location("",myloc.getX() + rx, myloc.getY() + ry));

        // wait for 0.01 seconds
        rplan.waitFor(100).addResultListener(new DelegationResultListener<Void>(ret) {
        //public void customResultAvailable(Void result)
        //{
        //updateVision().addResultListener(new DelegationResultListener<Void>(ret));
        //}
        });
        return ret;
    }


    //Methode 1 + Parameters added -oemer
    /*
    - Methode 1: Batterieverbrauch
    Input: Distanz von Matsim, Output verbrauch/ändert den Battereistand und andere Parameter
    die entweder in die Beliefs reinkommen oder in der BatteryModel.java bleiben).
     Diese Methode bei Sensoryupdate aufrubar machen
*/

    private BatteryModel linkEnterEventsMap;
    private Id<Vehicle> specificVehicleId;
    VehicleTrackingData trackingData = linkEnterEventsMap.get(specificVehicleId);

    private VehicleTrackingData get(Id<Vehicle> specificVehicleId) {
        return null;
    }
    //This is the distance traveled since the vehicle agent started.
    double distanceTraveled = trackingData.getDistanceSinceStart();
    private static void decreaseBatteryHealthBasedOnDistance(BatteryModel batteryModel, Id<Vehicle> specificVehicleId) {

     //  VehicleTrackingData trackingData = EvacAgentTracker.linkEnterEventsMap.get(specificVehicleId);
        EvacAgentTracker evacAgentTracker = null;
        VehicleTrackingData trackingData = evacAgentTracker.linkEnterEventsMap.get(specificVehicleId);


        // Calculate distance traveled
        double distanceTraveled = 0.0;
        if (trackingData != null) {
            distanceTraveled = trackingData.getDistanceSinceStart();
            System.out.println("Distance traveled by the specific vehicle: " + distanceTraveled + " meters");
        } else {
            System.out.println("Tracking data not found for the specific vehicle.");
        }

        //decreasing battery health based on distance traveled -oemer
        double healthDecreaseCoefficient = 0.0001;
        double healthDecrease = healthDecreaseCoefficient * distanceTraveled;
        double newBatteryHealth = batteryModel.getMyBatteryHealth() - healthDecrease;
        batteryModel.setMyBatteryHealth(newBatteryHealth);
    }
    private void setMyBatteryHealth(double newBatteryHealth) {
    }
    private double getMyBatteryHealth() {
        return 0;
    }

}