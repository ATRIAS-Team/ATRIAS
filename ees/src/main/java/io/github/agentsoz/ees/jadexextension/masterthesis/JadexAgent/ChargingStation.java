package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.util.Location;

public class ChargingStation {

    protected static int instancecnt = 0;
    private Location location;

    /**
     *  Get an instance number.
     */
    protected static synchronized int	getNumber()
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
       // commented out -oemer
        // setId(name);
        setName(name);
        //commented out -oemer
     //   setLocation(location);
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
        //-commented out -oemer
     //   assert this.getId().equals(st.getId());
    }

    //-------- object methods --------

    /**
     *  Get a string representation of this Chargingstation.
     *  @return The string representation.
     */
    public String toString()
    {
        // commented out-oemer
     //   return "Chargingstation(" + "id=" + getId() + ", location=" + getLocation() + ", name=" + getName() + ")";
     return "";
    }

    public Location getLocation()
    {
        return this.location;
    }
}
