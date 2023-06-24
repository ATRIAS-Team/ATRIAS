package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;

import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.service.annotation.OnStart;
import jadex.micro.annotation.Agent;
import jadex.quickstart.cleanerworld.environment.*;
import jadex.quickstart.cleanerworld.gui.SensorGui;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 *  BDI agent template.
 */
@Agent(type="bdi")    // This annotation makes the java class and agent and enabled BDI features
public class CleanerBDIAgentA0 {
    //-------- fields holding agent data --------

    /**
     * The sensor/actuator object gives access to the environment of the cleaner robot.
     */
    private SensorActuator actsense = new SensorActuator();

    @Belief
    private ICleaner self = actsense.getSelf();

    /**
     * Set of the known charging stations. Managed by SensorActuator object.
     */
    @Belief
    private Set<IChargingstation> stations = new LinkedHashSet<>();

    /**
     * Set of the known waste bins. Managed by SensorActuator object.
     */
    @Belief
    private Set<IWastebin> wastebins = new LinkedHashSet<>();

    /**
     * Set of the known waste items. Managed by SensorActuator object.
     */
    @Belief
    private Set<IWaste> wastes = new LinkedHashSet<>();
    //... add more field here

    //-------- setup code --------

    /**
     * The body is executed when the agent is started.
     *
     * @param bdifeature Provides access to bdi specific methods
     */
    @OnStart    // This annotation informs the Jadex platform to call this method once the agent is started
    private void exampleBehavior(IBDIAgentFeature bdi) {
        // Open a window showing the agent's perceptions
        new SensorGui(actsense).setVisible(true);

        //... add more setup code here
       // actsense.moveTo(Math.random(), Math.random());    // Dummy call so that the cleaner moves a little.
        IWaste waste = wastes.iterator().next();
        bdi.dispatchTopLevelGoal(new AvoidWaste(waste));
    }


    //-------- additional BDI agent code --------

    //... BDI goals and plans will be added here
    @Goal
    class AvoidWaste
    {
        IWaste waste;

        @GoalCreationCondition(factadded = "wastes")
        public AvoidWaste(IWaste waste) {
            this.waste = waste;
        }
        @GoalTargetCondition
        boolean	WasteAvoided()
        {
            // Test if the waste is not believed to be in the environment
            return wastes.contains(waste);
        }
    }


    @Plan(trigger = @Trigger(goals = AvoidWaste.class))
    private void performPatrolPlan2() {
        // Follow another path around the middle of the museum.
        System.out.println("Starting performPatrolPlan2()");
        actsense.moveTo(0.3, 0.3);
    }
}

