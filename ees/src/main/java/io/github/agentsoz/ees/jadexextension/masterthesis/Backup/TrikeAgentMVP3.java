package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;


import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.bdiabm.Agent;
import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexModel.JadexModel;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.OnStart;
import jadex.micro.annotation.Description;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;


@jadex.micro.annotation.Agent(type=BDIAgentFactory.TYPE)
@Description("<h1>Trike Robot Agent</h1>")

public class TrikeAgentMVP3 implements EnvironmentActionInterface, Agent

{

	/** The bdi agent. Automatically injected */
	@jadex.micro.annotation.Agent
	private IInternalAccess agent;

	private Agent trikeProxy;

//	private final Logger logger = LoggerFactory.getLogger(TrikeAgentMVP.class);

	private int reactionTimeInSecs = 30;

	private AgentDataContainer outAdc;
	private EnvironmentActionInterface envActionInterface;



	//public int reactionTimeInSecs = 30;

	enum State {
		responseThresholdInitialReached,
		responseThresholdFinalReached,
		//
		status,
		isStuck,
	}
	enum Beliefname {
		Age("Age"),
		AgentId("AgentId"),
		Archetype("Archetype"),
		ArchetypeAge("Archetypes.Age"),
		ArchetypeHousehold("Archetypes.Household"),
		AgentType("BDIAgentType"),
		Address("EZI_ADD"),
		Gender("Gender"),
		AddressCoordinates("Geographical.Coordinate"),
		HasDependents("HasDependents"),
		HasDependentsAtLocation("HasDependentsAtLocation"),
		HouseholdId("HouseholdId"),
		isDriving("isDriving"),
		Id("Id"),
		ImpactFromFireDangerIndexRating("ImpactFromFireDangerIndexRating"),
		ImpactFromImmersionInSmoke("ImpactFromImmersionInSmoke"),
		ImpactFromMessageAdvice("ImpactFromMessageAdvice"),
		ImpactFromMessageEmergencyWarning("ImpactFromMessageEmergencyWarning"),
		ImpactFromMessageEvacuateNow("ImpactFromMessageEvacuateNow"),
		ImpactFromMessageRespondersAttending("ImpactFromMessageRespondersAttending"),
		ImpactFromMessageWatchAndAct("ImpactFromMessageWatchAndAct"),
		ImpactFromSocialMessage("ImpactFromSocialMessage"),
		ImpactFromVisibleEmbers("ImpactFromVisibleEmbers"),
		ImpactFromVisibleFire("ImpactFromVisibleFire"),
		ImpactFromVisibleResponders("ImpactFromVisibleResponders"),
		ImpactFromVisibleSmoke("ImpactFromVisibleSmoke"),
		LagTimeInMinsForInitialResponse("LagTimeInMinsForInitialResponse"),
		LagTimeInMinsForFinalResponse("LagTimeInMinsForFinalResponse"),
		LocationEvacuationPreference("EvacLocationPreference"),
		LocationInvacPreference("InvacLocationPreference"),
		LocationHome("home"),
		LocationWork("work"),
		PrimaryFamilyType("PrimaryFamilyType"),
		ResponseThresholdFinal("ResponseThresholdFinal"),
		ResponseThresholdInitial("ResponseThresholdInitial"),
		Sa1("SA1_7DIGCODE"),
		Sa2("SA2_MAINCODE"),
		WillGoHomeAfterVisitingDependents("WillGoHomeAfterVisitingDependents"),
		WillGoHomeBeforeLeaving("WillGoHomeBeforeLeaving"),
		WillStay("WillStay"),
		;

		private final String commonName;

		Beliefname(String name){
			this.commonName = name;
		}

		public String getCommonName() {
			return commonName;
		}
	};

	enum StatusValue {
		to,
		at,
	}

	private PrintStream writer = null;
	private double time = -1;
	//private TrikeAgent.Prefix prefix = new TrikeAgent.Prefix();
	private Random rand = new Random(0);

	private Location dependentsLocation;
	private Location evacLocation;
	private Location invacLocation;
	private Location homeLocation;
	private Location workLocation;
	private Location[] stuckLocation;

	private double responseThresholdInitial = 1.0;
	private double responseThresholdFinal = 1.0;

	private double futureValueOfFireDangerIndexRating = 0.0;
	private double futureValueOfVisibleFire = 0.0;
	private double futureValueOfVisibleSmoke = 0.0;
	private double futureValueOfSmokeImmersion = 0.0;
	private double futureValueOfVisibleEmbers = 0.0;
	private double futureValueOfVisibleResponders = 0.0;
	private double futureValueOfMessageRespondersAttending = 0.0;
	private double futureValueOfMessageAdvice = 0.0;
	private double futureValueOfMessageWatchAndAct = 0.0;
	private double futureValueOfMessageEmergencyWarning = 0.0;
	private double futureValueOfMessageEvacuateNow = 0.0;
	private double futureValueOfMessageSocial = 0.0;
	//
	private double anxietyFromSituation = 0.0;
	private double anxietyFromEmergencyMessages = 0.0;
	private double anxietyFromSocialMessages = 0.0;

	private double testCoord[] = {239869.746605, 5892499.730682};

	private Map<String,EnvironmentAction> activeBdiActions = new HashMap<>();
	private String name;

	//private Map<String,EnvironmentAction> activeBdiActions;

	// Last BDI action done (used for checking status of finished drive actions)
	//EnvironmentAction lastBdiAction;
	//ActionContent.State lastBdiActionState;
	public TrikeAgentMVP3() {
		outAdc = new AgentDataContainer();
		setEnvironmentActionInterface(this);}
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body()
	{

		System.out.println("HelloWorld");



		//From ArchetypeAgent.java: Goal prepareDrivingGoal()

		//JadexEnvironmentAction action = new JadexEnvironmentAction(
		//		Integer.toString(0),
		//		Constants.DRIVETO, params);
		//addActiveEnvironmentAction(action); // will be reset by updateAction()
		//return action;



	//	trikeProxy.getEnvironmentActionInterface();
				//.packageAction("0", "drive_to", params, " ");
		//JadexEnvironmentAction.packageTest();
	//	((Agent) agent).getEnvironmentActionInterface().packageAction("0", "drive_to", params, null);

		//JadexModel modelTest = new JadexModel();
		//modelTest.packageAction("0", "drive_to", params, null);

	//	io.github.agentsoz.bdiabm.Agent.getEnvironmentActionInterface().packageAction("0", "drive_to", params, null);

		//ActionContent ac = new ActionContent(params, ActionContent.State.INITIATED, "drive_to");
		//AgentDataContainer.putAction("0", "drive_to", ac);



	}


//	public void start(PrintStream writer, String[] params) {

		// register to perceive certain events
	//	EnvironmentAction action = new EnvironmentAction(
	//			Integer.toString(getId()),
	//	Constants.PERCEIVE,
		//		new Object[] {
				//		Constants.BLOCKED,
					//	Constants.CONGESTION,
						//Constants.ARRIVED,
					//	Constants.DEPARTED,
					//	Constants.ACTIVITY_STARTED,
					//	Constants.ACTIVITY_ENDED,
					//	Constants.STUCK});
		//post(action);
		//addActiveEnvironmentAction(action);
//	}

	public void Addingtoadc() {
		String agentID = "6";
		String actionID = "perceive";
		Object[] params = new Object[7];
		params[0] = "blocked";
		params[1] = "congestion";
		params[2] = "arrived"; // five secs from now;
		params[3] = "departed";
		params[4] = "activity_started";
		params[5] = "activity_ended"; // add replan activity to mark location/time of replanning
		params[6] = "stuck";
		Object[] actionparameters = params;
		getEnvironmentActionInterface().packageAction(agentID, actionID, actionparameters, "");
	}


		@Override
	public void packageAction(String agentId, String actionID, Object[] parameters, String actionState) {
		ActionContent.State state = ActionContent.State.INITIATED;
		if (actionState != null) {
			try {
				state = ActionContent.State.valueOf(actionState);
			} catch (Exception e) {
				//logger.warn("agent {} ignoring unknown action state {}", agentId, actionState);
			}
		}
		ActionContent ac = new ActionContent(parameters, state, actionID );
		outAdc.putAction(agentId, actionID, ac);

		JadexModel.outAdcincycle.putAction(agentId, actionID, ac);
	//	copy(outAdc, JadexModel.outAdc);
		outAdc = null;

	}

//	private void addActiveEnvironmentAction(EnvironmentAction activeEnvironmentAction) {
//		activeBdiActions.put(activeEnvironmentAction.getActionID(), activeEnvironmentAction);


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

	//@Override
	public void init(String[] args) {

	}

	//@Override
	public void start() {

	}

	//@Override
	public void handlePercept(String perceptID, Object parameters) {

	}

	//Agent agent = new


	//@Override
	public void updateAction(String actionID, ActionContent content) {
		//logger.debug("{} received action update: {}", logPrefix(), content);
		ActionContent.State actionState = content.getState();
		if (actionState == ActionContent.State.PASSED ||
				actionState == ActionContent.State.FAILED ||
				actionState == ActionContent.State.DROPPED) {


			//memorise(BushfireAgent.MemoryEventType.BELIEVED.name(),
			//        BushfireAgent.MemoryEventValue.LAST_ENV_ACTION_STATE.name() + "=" + actionState.name());


			// remove the action and records it as the last action performed
		//	EnvironmentAction lastAction = removeActiveEnvironmentAction(content.getAction_type());
			//setLastBdiActionAndState(lastAction, actionState);
			if (content.getAction_type().equals(Constants.DRIVETO)) {
				// Wake up the agent that was waiting for drive action to finish
			//	suspend(false);
			}
		}
	}

	//@Override
	public void kill() {

	}

	//@Override
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {

	}

	//@Override
	public QueryPerceptInterface getQueryPerceptInterface() {
		return null;
	}

	//@Override
	public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface) {
		this.envActionInterface = envActInterface;
	}

	//@Override
	public EnvironmentActionInterface getEnvironmentActionInterface() {
		return envActionInterface;
	}

	//private EnvironmentAction removeActiveEnvironmentAction(String actionId) {
	//	if (actionId != null && activeBdiActions.containsKey(actionId)) {
	//		return activeBdiActions.remove(actionId);
	//	}
	///	return null;
//	}

	public Agent getTrikeABMProxy(){

		return trikeProxy;
	}


}
