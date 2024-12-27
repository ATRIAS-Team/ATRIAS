package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Message;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.search.ServiceQuery;

public interface IAreaTrikeService
{
    //  gets a service depending on agent and message receiver tag
    static IAreaTrikeService messageToService(IInternalAccess agent, Message message) {
        ServiceQuery<IAreaTrikeService> query = new ServiceQuery<>(IAreaTrikeService.class);
        query.setScope(ServiceScope.PLATFORM);
        String receiverId = message.getReceiverId();
        query.setServiceTags(receiverId); // calling the tag of a trike agent
        IAreaTrikeService service = agent.getLocalService(query);
        return service;
    }

    void sendMessage(String messageStr);
}