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

import io.github.agentsoz.util.Location;

public class ChargingStation {

    protected String id;
    protected static int instancecnt = 0;
    protected Location location;
    /**
     *  Get an instance number.
     */
    protected static synchronized int getNumber()
    {
        return ++instancecnt;
    }

    //-------- attributes ----------

    /** Attribute for slot name. */
    protected String name;

    //-------- constructors --------

    /**
     *  Create a new Chargingstation.
     */
    public ChargingStation()
    {
        // Empty constructor required for JavaBeans (do not remove).
    }
    /**
     *  Create a new charging station.
     */
    public ChargingStation(Location location)
    {
        this("Charging station #" + getNumber(), location);
    }

    /**
     *  Create a new Chargingstation.
     */
    public ChargingStation(String name, Location location)
    {

        setId(name);
        setName(name);
        setLocation(location);
    }

public void setLocation(Location location){
        this.location = location;

}

    public void setId(String id){
        this.id = id;
    }

    /**
     *  Get the name of this Chargingstation.
     * @return name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     *  Set the name of this Chargingstation.
     * @param name the value to be set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     *  Update this destination.
     */
    public void update(ChargingStation st)
    {

     //   assert this.getId().equals(st.getId());
    }

    //-------- object methods --------

    /**
     *  Get a string representation of this Chargingstation.
     *  @return The string representation.
     */
    public String toString()
    {
        return "Chargingstation(" + "id=" + getId() + ", location=" + getLocation() + ", name=" + getName() + ")";
    }

    public String getId()
    {
        return this.id;
    }

    public Location getLocation()
    {
        return this.location;
    }
}
