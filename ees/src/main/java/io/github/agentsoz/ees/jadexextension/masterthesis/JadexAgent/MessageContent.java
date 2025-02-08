package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

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

import java.util.ArrayList;

public class MessageContent {
    String action;
    public ArrayList<String> values;

    public MessageContent(String action, ArrayList<String> values){
        this.action = action;
        this.values = values;
    }

    public MessageContent(String action){
        this.action = action;
        this.values = new ArrayList();

    }

    public String getAction(){
        return this.action;
    }

    public ArrayList<String> getValues(){
        return this.values;
    }
    /*
    public String serialize(){

        String valuesStr = "[";
        for (int i = 0; i < values.size() - 1; i++) {
            valuesStr += values.get(i) + ",";
        }
        valuesStr += values.get(values.size() - 1) + "]";
        return "{"+"action:"+action+","+"values:"+valuesStr+"}";
         *
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static MessageContent deserialize(String messageStr){
        String[] parts = messageStr.split(",(?=values)");
        String action = parts[0].split(":")[1];
        ArrayList<String> values = (ArrayList<String>) Arrays.asList(parts[1].split(","));
        return new MessageContent(action, values);
    }
    */
}
