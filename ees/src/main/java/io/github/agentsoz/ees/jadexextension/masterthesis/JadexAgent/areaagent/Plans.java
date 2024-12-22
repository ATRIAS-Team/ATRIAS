package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.AreaAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Cells;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Message;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.MessageContent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;

import java.time.Instant;

public class Plans {
    private AreaAgent areaAgent;
    private Utils utils;
    public Plans(AreaAgent areaAgent, Utils utils){
        this.areaAgent = areaAgent;
        this.utils = utils;
    }
    public void checkAreaMessagesBuffer(){
        if(areaAgent.areaMessagesBuffer.isEmpty()) return;
        Message bufferMessage = areaAgent.areaMessagesBuffer.read();
        String areaId = bufferMessage.getSenderId();

        if(areaAgent.locatedAgentList.size() < areaAgent.MIN_TRIKES) return;
        MessageContent messageContent = new MessageContent("PROPOSE");
        messageContent.values.addAll(bufferMessage.getContent().getValues());
        messageContent.values.add(areaAgent.cell);

        Message message = new Message("0", areaAgent.areaAgentId, areaId, "request", JadexModel.simulationtime, messageContent);
        IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
        service.receiveMessage(message.serialize());
    }

    public void checkProposals(){
        for (DelegateInfo delegateInfo: areaAgent.jobsToDelegate) {
            long currentTime = Instant.now().toEpochMilli();
            if(delegateInfo.ts == -1 || currentTime < delegateInfo.ts + areaAgent.waitTime) return;
            if(areaAgent.proposalBuffer.isEmpty()){
                throw new RuntimeException("FAILED TRIP");
            }

            long minHops = 10;
            String bestAreaAgent = null;

            while (!areaAgent.proposalBuffer.isEmpty()){
                Message bufferMessage = areaAgent.proposalBuffer.read();
                String areaId = bufferMessage.getSenderId();
                String areaCell = bufferMessage.getContent().values.get(10);

                long hops = Cells.getHops(areaAgent.cell, areaCell);
                if(hops < minHops){
                    minHops = hops;
                    bestAreaAgent = areaId;
                }
            }

            MessageContent messageContent = new MessageContent("ASSIGN");
            messageContent.values = delegateInfo.job.toArrayList();
            Message message = new Message("0", areaAgent.areaAgentId, bestAreaAgent, "request", JadexModel.simulationtime, messageContent);
            IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
            service.receiveMessage(message.serialize());

            areaAgent.jobsToDelegate.remove(delegateInfo);
        }
    }

}
