package io.github.agentsoz.ees.JadexService.MappingService;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import io.github.agentsoz.ees.simagent.SimSensoryInputBroker;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.util.HashMap;

/**
 * Mapping service implementation.
 */
@Service
public class WritingBrokerIDService implements IMappingInputBrokerService {
    //-------- attributes --------

    /**
     * The agent.
     */
    @ServiceComponent
    protected IInternalAccess agent;

    public HashMap AgentMap;

    //-------- attributes --------	
    /**
     * 01.02.2023: Writting the agent ID to each Jadex Agent `s AgentID argument
     */
    public void MapSensoryInput(final HashMap MapSensoryInputMap) {
        final SimSensoryInputBroker SensoryInput = (SimSensoryInputBroker) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();

        // Getting the AgentID for the Agent from the Agent Map
        String agentID = (String) MapSensoryInputMap.get(agent.getId().getName());
        SensoryInput.setSensoryInputID(agentID);

    }
}
