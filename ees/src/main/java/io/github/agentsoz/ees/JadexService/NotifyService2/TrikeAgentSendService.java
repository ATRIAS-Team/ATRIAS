package io.github.agentsoz.ees.JadexService.NotifyService2;

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

import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

/**
 *  Chat service implementation.
 */
@Service
public class TrikeAgentSendService implements INotifyService2 {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	//-------- attributes --------	

	public void NotifyotherAgent(String AgentID)
	{

	}


	public void removeTrikeAgentfromActiveList(String AgentID)
	{

	}
}
