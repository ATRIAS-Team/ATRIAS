package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.matsim.EvacAgentTracker;
import io.github.agentsoz.ees.matsim.EvacAgentTracker.VehicleTrackingData;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.runtime.IPlan;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.jdeqsim.Vehicle;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import java.util.List;

public class BatteryModel {

    protected double my_numberofcharges = 200;
    protected double my_batteryhealth = 1.0 - 0.00025 * my_numberofcharges;
    protected double my_speed;
    protected boolean my_autopilot;
    protected int carriedcustomer;
    protected TrikeAgent capa;
    protected ChargingStation stat;
    protected IPlan rplan;
    //private double lastUpdateTime = 0.0; //why here???
    public final double DEFAULT_TOLERANCE = 0.001;
    public double my_chargestate = 0.9; // TrikeAgent.java
    protected boolean daytime = true; // TrikeAgent.java
    //protected static TrikeAgent.AchieveMoveTo goal;

    //Methode 1: Batterieverbrauch -oemer
    /*
    Input: Distanz von Matsim nehmen, Output verbrauch/ändert den Batteriestand und andere Parameter
    - Diese Methode wird bei SensoryUpdate in TrikeAgent.java aufgerufen
    */
    // private BatteryModel linkEnterEventsMap;
    // VehicleTrackingData trackingData = linkEnterEventsMap.get(specificVehicleId);
    // private Id<Vehicle> specificVehicleId;
    // private VehicleTrackingData get(Id<Vehicle> specificVehicleId) {
    //     return null;
    // }
    //private static void decreaseBattery(BatteryModel batteryModel, Id<Vehicle> specificVehicleId) {

    public void discharge(double metersDriven, int carriedcustomer){

        // EvacAgentTracker evacAgentTracker = null;
        // VehicleTrackingData trackingData = evacAgentTracker.linkEnterEventsMap.get(specificVehicleId);
        // double distanceTraveled = 0.0;
        // if (trackingData != null) {
        //This is the distance traveled since the vehicle agent started -oemer
        //Link tripLink = null;
        //distanceTraveled = tripLink.getLength();

        //   distanceTraveled = trackingData.getDistanceSinceStart();
        System.out.println("Distance traveled by the specific vehicle: " + metersDriven + " meters");
        //  } else {
        //     System.out.println("Tracking data not found for the specific vehicle.");
        // }
        //decreasing battery health based on distance traveled -oemer
        // double healthDecreaseCoefficient = 0.0001;
        // double healthDecrease = healthDecreaseCoefficient * metersDriven;
        // double newBatteryHealth = getMyBatteryHealth() - healthDecrease;
        // setMyBatteryHealth(newBatteryHealth);

        double chargeDecreaseCoefficient = 0.0001;
        double chargeDecrease = chargeDecreaseCoefficient * metersDriven;
        double newChargingState = getMyChargestate() - chargeDecrease;
        setMyChargestate(newChargingState);


    }

    public Double SimulateDischarge(double metersDriven){
        double chargeDecreaseCoefficient = 0.0001;
        double chargeDecrease = chargeDecreaseCoefficient * metersDriven; //TODO: paper
        //double newChargingState = getMyChargestate() - chargeDecrease;
        return chargeDecrease;
    }

    //Methode 2 added -oemer

    public void loadBattery()
    {
        //Hier sucht sich der Trike Agent eine Station raus und fährt mit AchieveMoveTo zur Station. Das soll als
        // Fahrauftrag erledigt werden.
        //commented out from ekins thesis -oemer
        //   Chargingstation station = ((io.github.agentsoz.ees.jadexextension.masterthesis.
        //   trike.TrikeAgent.QueryChargingStation)
        //   planapi.dispatchSubgoal(agentapi.new QueryChargingStation()).get()).getStation();
        //   planapi.dispatchSubgoal(agentapi.new AchieveMoveTo(station.getLocation())).get();
        //TODO: Location of trike agent and location of Charging station
        // while (charge<1.0 && agentapi.getLocation().getDistance(station.getLocation())<0.01)

        double charge = getMyChargestate();
        double batteryhealth = getMyBatteryHealth();
        double numberofcharges = getMyNumberOfCharges();

        //create a new ChargingStation  -oemer
        //ChargingStation station = new ChargingStation();
        //TODO oemer: while (charge <1 && BatteryModel.getLocation().getDistance(station.getLocation()) <0.01) prüft,
        // ob Batterymodel bei Chargingstation ist.
        //while (charge<1 && trikeAgent.location == chargingStation.location() )
        //while (charge <1)

        {
            // Daytime
            if (daytime)
            {
                charge = Math.min(charge + 0.01, 1.0);

                if (charge>0.99)
                {
                    numberofcharges = 0;
                    numberofcharges = numberofcharges + 1;
                }
            }
            // Nighttime
            else
            {
                charge = Math.min(charge + 0.005, 1.0);

                if (charge>0.995)
                {
                    numberofcharges = 0;
                    numberofcharges = numberofcharges + 0.5;
                }
            }
            updateChargingProgress();
            //setMyChargestate(charge); //why?
            setMyChargestate(1.0); //workaround @Marcel
            //TODO: what does this code do?? -oemer
            // IPlan planapi = null;
            // planapi.waitFor(100).get();
            setMyNumberOfCharges(numberofcharges);
            batteryhealth = 1.0 - 0.00025 * numberofcharges;
            setMyBatteryHealth(batteryhealth);
        }
    }

    /*
 - Charge als Methode in Einklang mit Simulationszeit bringen(DONE)
 - ChargingStation.java als Liste in den Agenten, oder eine eigene Klasse, oder als Agent definieren
 - Trike Agent.java: z. 713 Atchargingstation
*/
    /**
    private void updateChargingProgress() {
        double chargingRate = 0.001;
        while (my_chargestate < 1.0) {
            double currentSimTime = JadexModel.simulationtime;
            double SimTimeDelta = currentSimTime - lastUpdateTime;
            double newChargeState = my_chargestate + chargingRate * SimTimeDelta;
            my_chargestate = Math.min(newChargeState, 1.0);
            lastUpdateTime = currentSimTime;
        }
    }**/

    //new version @Marcel
    //calculation of delta corrected
    private void updateChargingProgress() {
        double chargingRate = 0.001;
        double currentSimTime = JadexModel.simulationtime;
        double lastUpdateTime = JadexModel.simulationtime;
        Double SimTimeDelta;
        double newChargeState;

        my_chargestate = 1.0;
        /** while loop prevent JadexModel.simulationtime to update
        while (my_chargestate < 1.0) {
            currentSimTime = JadexModel.simulationtime;
            SimTimeDelta = currentSimTime - lastUpdateTime;
            if (SimTimeDelta > 0.0){
                my_chargestate = my_chargestate + chargingRate * SimTimeDelta;
                lastUpdateTime = currentSimTime;
            }
            //mit vergangener zeit arbeiten? nur eien variable ind er schleife neu setzen!
        }
        my_chargestate = Math.min(my_chargestate, 1.0);
         **/
    }


    protected IFuture<Void> moveToTarget() {
        final Future<Void> ret = new Future<Void>();

        Location target = stat.getLocation();
        Location myloc = capa.agentLocation; //Location of the TrikeAgent

        if (!(myloc == target)) { //comparison to other Location //omit isnear method oemer
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


    protected IFuture<Void> oneStepToTarget() {

        final Future<Void> ret = new Future<Void>();

        //Here the distance is calculated between 2 points by using a L1 norm
        Location target = stat.getLocation();
        Location myloc = capa.agentLocation;

        double speed = getMySpeed();
        boolean autopilot = getMyAutopilot();
        double charge = getMyChargestate();
        double batteryhealth = getMyBatteryHealth();
        int carriedcustomer = getCarriedCustomer();


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

        setMySpeed(speed);
        setMyChargestate(Double.valueOf(charge));

        double d = myloc.distanceBetween(myloc, target);
        double r = speed * 0.004;//(newtime-time);
        double dx = target.getX() - myloc.getX();
        double dy = target.getY() - myloc.getY();

        double rx = r < d ? r * dx / d : dx;
        double ry = r < d ? r * dy / d : dy;
        System.out.println("mypos: "+(myloc.getX()+rx)+" "+(myloc.getY()+ry)+" "+target);
        capa.setMyLocation(new Location("",myloc.getX() + rx, myloc.getY() + ry));

        // wait for 0.01 seconds
        rplan.waitFor(100).addResultListener(new DelegationResultListener<Void>(ret) {
        });
        return ret;
    }

    //methods added from Ekins' trike agent -oemer
    public double getMyChargestate()
    {
        return my_chargestate;
    }
    public void setMyChargestate(double my_chargestate)
    {
        this.my_chargestate = my_chargestate;
    }
    public double getMyBatteryHealth() {
        return my_batteryhealth;
    }
    public void setMyBatteryHealth(double my_batteryhealth) {
        this.my_batteryhealth = my_batteryhealth;
    }
    public double getMyNumberOfCharges() {
        return my_numberofcharges;
    }
    public void setMyNumberOfCharges(double my_numberofcharges) {
        this.my_numberofcharges = my_numberofcharges;
    }
    public int getCarriedCustomer()
    {
        return carriedcustomer;
    }
    public boolean getMyAutopilot()
    {
        return my_autopilot;
    }
    public void setMyAutopilot(boolean my_autopilot)
    {
        this.my_autopilot = my_autopilot;
    }
    public double getMySpeed()
    {
        return my_speed;
    }
    public void setMySpeed(double my_speed)
    {
        this.my_speed = my_speed;
    }

    //  public double x;

    //  public double y;

    //added -oemer
    //  public double getDistance(Location other)
    //  {
    //      assert other != null;
    //      return Math.sqrt((other.y - this.y) * (other.y - this.y) + (other.x - this.x) * (other.x - this.x));
    //  }
    /**
     *  Check, if two locations are near to each other
     *  using the default tolerance.
     *  @return True, if two locations are near to each other.
     */

    //added -oemer
    // public boolean isNear(Location other)
    // {
    //     return isNear(other, DEFAULT_TOLERANCE);
    // }
    //added -oemer
    // public boolean isNear(Location other, double tolerance)
    // {
    //     return getDistance(other) <= tolerance;
    // }
}