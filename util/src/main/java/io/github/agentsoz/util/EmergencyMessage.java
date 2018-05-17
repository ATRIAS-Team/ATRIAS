package io.github.agentsoz.util;

/*-
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
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

import com.google.gson.Gson;

import java.util.Map;

public class EmergencyMessage {

    public enum EmergencyMessageType {
        ADVICE,
        WATCH_AND_ACT,
        EVACUATE_NOW
    }

    private EmergencyMessageType type;

    private String content;
    private String broadcastHHMM;
    private Map<String,Double[][]> broadcastZones;

    public EmergencyMessage(EmergencyMessageType type,
                            String content,
                            String broadcastHHMM,
                            Map<String,Double[][]> broadcastZones) {
        this.type = type;
        this.content = content;
        this.broadcastHHMM = broadcastHHMM;
        this.broadcastZones = broadcastZones;
    }

    public EmergencyMessageType getType() {
        return type;
    }

    public String getBroadcastHHMM() {
        return broadcastHHMM;
    }

    public Map<String,Double[][]> getBroadcastZones() {
        return broadcastZones;
    }

    public String getContent() { return content; }

    public void setBroadcastZones(Map<String, Double[][]> broadcastZones) {
        this.broadcastZones = broadcastZones;
    }

    @Override
    public String toString() {
        return (new Gson()).toJson(this);
    }
}
