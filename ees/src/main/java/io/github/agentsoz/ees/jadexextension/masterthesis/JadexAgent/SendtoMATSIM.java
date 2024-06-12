package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.agents.archetype.ArchetypeAgent;
import io.github.agentsoz.util.Location;

/**
 *  The chat service interface.
 */
public interface SendtoMATSIM
{
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void sendDriveTotoAdc();

	public void SendPerceivetoAdc();

	public double getDrivingDistanceTo(Location location) throws AgentNotFoundException;;


}

