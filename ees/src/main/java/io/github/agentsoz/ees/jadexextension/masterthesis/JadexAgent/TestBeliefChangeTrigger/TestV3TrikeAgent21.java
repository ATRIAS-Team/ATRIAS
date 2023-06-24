package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TestBeliefChangeTrigger;



import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.*;

import java.util.Collection;
import java.util.Iterator;

@Agent(type = "bdi")
@ProvidedServices(@ProvidedService(type= ITestBeliefChangeService.class, implementation=@Implementation(SupportAgentService.class)))
@RequiredServices({
		@RequiredService(name = "testbeliefchangeservices", type = ITestBeliefChangeService.class, scope = ServiceScope.PLATFORM),
		@RequiredService(name = "clockservice", type = IClockService.class)
})


//Test TrikeAgent if they could write into the belief of Support Agent via service and support agent could react correctly to that

public class TestV3TrikeAgent21 {

	/**
	 * The bdi agent. Automatically injected
	 */
	@Agent
	private IInternalAccess agent;
	@AgentFeature
	protected IBDIAgentFeature bdiFeature;

	@AgentFeature
	protected IRequiredServicesFeature requiredServicesFeature;

	@AgentFeature
	protected IExecutionFeature execFeature;

	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {


		bdiFeature.dispatchTopLevelGoal(new AccesstoSupportAgentBelief());


		System.out.println("sucessully dispatch goals");


	}

	/**
	 * 16.02 test to see if an agent could just write directly in the belief of the other?
	 **/
	@Goal(excludemode = ExcludeMode.Never)
	class AccesstoSupportAgentBelief {
		public AccesstoSupportAgentBelief() {
		}
	}




	@Plan(trigger = @Trigger(goals = AccesstoSupportAgentBelief.class))
	public class WriteintoBeliefPlan {

		public WriteintoBeliefPlan() {
		}

		@PlanBody
		public void WriteintoSAgent() {
			IFuture<Collection<ITestBeliefChangeService>> chatservices = requiredServicesFeature.getServices("testbeliefchangeservices");
			chatservices.addResultListener(new DefaultResultListener<Collection<ITestBeliefChangeService>>() {
				public void resultAvailable(Collection<ITestBeliefChangeService> result) {
					for (Iterator<ITestBeliefChangeService> it = result.iterator(); it.hasNext(); ) {
						ITestBeliefChangeService cs = it.next();
							cs.WriteinSAgent("Hello2");
					}

				}
			});
		}
	}

}










