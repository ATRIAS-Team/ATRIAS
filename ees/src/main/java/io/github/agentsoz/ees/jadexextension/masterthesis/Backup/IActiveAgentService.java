package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;

/**
 *  The chat service interface.
 */
public interface IActiveAgentService
{
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void NotifyotherAgent(String sender, String text);


}

