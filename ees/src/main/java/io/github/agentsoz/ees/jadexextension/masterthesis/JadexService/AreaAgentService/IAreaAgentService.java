package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaAgentService;

/**
 *  The chat service interface.
 */
public interface IAreaAgentService
{
    /**
     *  Receives a chat message.
     *  @param sender The sender's name.
     *  @param text The message text.
     */
    //public void sendTrip(String text);
    public void sendAreaAgentUpdate(String text);

    void sendJob(String content);


    /**
     *  Basic chat user interface.
     */
}