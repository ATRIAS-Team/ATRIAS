package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *  Mapping service implementation.
 */
@Service
public class TrikeAgentService implements IAreaTrikeService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	public HashMap AgentMap;

	//-------- attributes --------

	///////////////////////////////////////////////////////
	//trike agent part

	//	receives jobs for trike
	public void trikeReceiveJob(String messageStr){
		final TrikeAgent trikeAgent = (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);
		Job job = new Job(messageObj.getContent().getValues());
		//trikeAgent.AddJobToJobList(job);
		DecisionTask decisionTask = new DecisionTask(job, messageObj.getSenderId(), "new");
		trikeAgent.AddDecisionTask(decisionTask);
	}

	//	trike to trike connectivity
	public void trikeReceiveTrikeMessage(String messageStr){
		final TrikeAgent trikeAgent = (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);


		if(messageObj.getComAct().equals("CallForProposal")) {
			Job job = new Job(messageObj.getContent().getValues());
			DecisionTask decisionTask = new DecisionTask(job, messageObj.getSenderId(), "proposed");
			trikeAgent.AddDecisionTask(decisionTask);
		}
		else if(messageObj.getComAct().equals("Propose")) {

			String jobID = messageObj.getContent().getValues().get(0);
			Double propose = Double.parseDouble(messageObj.getContent().getValues().get(2));
			String senderID = messageObj.getSenderId();

			for(int i=0; i<trikeAgent.decisionTaskList.size(); i++){
				if (jobID.equals(trikeAgent.decisionTaskList.get(i).getJobID())){
					//  agentID+score dazu speichern
					trikeAgent.decisionTaskList.get(i).setUtillityScore(senderID, propose);
				}

				//wenn job id übereinstimmt dann

			}

			//trikeAgent.setUtillityScore(senderID, propose);

			// in utscorelist speichern
		}
		else if(messageObj.getComAct().equals("AcceptProposal")) {
			String jobID = messageObj.getContent().getValues().get(0);
			for (int i = 0; i < trikeAgent.decisionTaskList.size(); i++) {
				if (jobID.equals(trikeAgent.decisionTaskList.get(i).getJobID())) {
					//  agentID+score dazu speichern
					trikeAgent.decisionTaskList.get(i).setStatus("commit");
					String timeStampBooked = new SimpleDateFormat("HH.mm.ss.ms").format(new java.util.Date());
					System.out.println("FINISHED Negotiation - JobID: " + trikeAgent.decisionTaskList.get(i).getJobID() + " TimeStamp: "+ timeStampBooked);
					ArrayList<String> values = new ArrayList<>();
					values.add(jobID);
					trikeAgent.sendMessage(messageObj.getSenderId(), "inform", "confirmAccept", values);
					// nur clients waitingForconfirmations
					//trikeAgent.testTrikeToTrikeService(messageObj.getSenderId(), "inform", "confirmAccept", values);
					// alle waitingmanager waiting confirmation
				}

			}
		}
		else if(messageObj.getComAct().equals("RejectProposal")){
			String jobID = messageObj.getContent().getValues().get(0);
			for (int i = 0; i < trikeAgent.decisionTaskList.size(); i++) {
				if (jobID.equals(trikeAgent.decisionTaskList.get(i).getJobID())) {
					//  agentID+score dazu speichern
					trikeAgent.decisionTaskList.get(i).setStatus("notAssigned");

				}

			}
		}
		//todo: seems to be never executed?
		else if(messageObj.getComAct().equals("inform")) {
			if(messageObj.getContent().getAction().equals("confirmAccept")){
				String jobID = messageObj.getContent().getValues().get(0);
				for (int i = 0; i < trikeAgent.decisionTaskList.size(); i++) {
					if (jobID.equals(trikeAgent.decisionTaskList.get(i).getJobID())) {
						//  agentID+score dazu speichern
						trikeAgent.decisionTaskList.get(i).setStatus("delegated");
						//todo: in erledigt verschieben hier oder im agent selbst
						trikeAgent.FinishedDecisionTaskList.add(trikeAgent.decisionTaskList.get(i));
						trikeAgent.decisionTaskList.remove(i);

					}

				}
			}

		}






		//trikeAgent.testModify();




	}

	//	receives the trikes in the area when area agent makes a response
	//todo: replace by a generic solution
	public void trikeReceiveAgentsInArea(String messageStr){
		final TrikeAgent trikeAgent = (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		System.out.println(messageStr);
		Message messageObj = Message.deserialize(messageStr);
		System.out.println(messageObj.getContent().getValues());
		System.out.println("JobID:" + messageObj.getContent().getValues().get(0));


		String JobIDSearch = messageObj.getContent().getValues().get(0);
		ArrayList<String> neighbourList = messageObj.getContent().getValues();
		neighbourList.remove(0); //JobID
		neighbourList.remove(0); //#

		Boolean found = false;
		Integer i = 0;
		while (found == false) {
			if (JobIDSearch.equals(trikeAgent.decisionTaskList.get(i).getJobID())){
				trikeAgent.decisionTaskList.get(i).setNeighbourIDs(neighbourList);
				trikeAgent.decisionTaskList.get(i).setStatus("readyForCFP");
				found = true;
			}
			else{
				i+=1;
			}
		}






		//todo: richtigen decision task bestimmen um einzufügen
		//todo: action vor absendne benennen
		//todo: ID von job beifügen

		//todo: @marcel hier in trike schreiben
	}

	///////////////////////////////////////////////////////
	//area agent part

	//DON'T PUT ANY CODE INSIDE METHODS
	public void areaReceiveUpdate(String message) {}

	public void receiveMessage(String messageStr) {}

	///////////////////////////////////////////////////////
}