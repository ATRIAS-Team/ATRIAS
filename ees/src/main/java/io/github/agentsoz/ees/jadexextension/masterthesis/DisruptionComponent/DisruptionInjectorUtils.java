package io.github.agentsoz.ees.jadexextension.masterthesis.DisruptionComponent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.DecisionTask;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class DisruptionInjectorUtils {
    private static <T> T findByProperty(Collection<T> col, Predicate<T> filter) {
        return col.stream().filter(filter).findFirst().orElse(null);
    }

    public static DisruptionInjector findAgentID(Collection<DisruptionInjector> toDisruptAgents, String agentID) {
        return findByProperty(toDisruptAgents, agent -> agentID.equals(agent.getToDisruptAgentID()));
    }

    public static DisruptionInjector findByDisruptionType(Collection<DisruptionInjector> toDisruptAgents, String disruptionType) {
        return findByProperty(toDisruptAgents, agent -> disruptionType.equals(agent.getDisruptionType()));
    }

    public static DisruptionInjector findByDisruptionTime(Collection<DisruptionInjector> toDisruptAgents, Integer disruptionTime) {
        return findByProperty(toDisruptAgents, agent -> disruptionTime.equals(agent.getDisruptionTime()));
    }

    public static boolean listContainsAgent(final List<DisruptionInjector> toDisruptAgents, final String agentID){
        return toDisruptAgents.stream().anyMatch(o -> o.getToDisruptAgentID().equals(agentID));
    }

}
