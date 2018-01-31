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

public class  Disruption {
    private String description;
    private String startHHMM;
    private String endHHMM;
    private double effectiveSpeed;
    private String effectiveSpeedUnit;
    private double[] latLon;
    private double impactRadiusMtrs;

    public Disruption(String description,
                      String startHHMM,
                      String endHHMM,
                      double effectiveSpeed,
                      String effectiveSpeedUnit,
                      double[] latLon,
                      double impactRadiusMtrs) {
        this.description = description;
        this.startHHMM = startHHMM;
        this.endHHMM = endHHMM;
        this.effectiveSpeed = effectiveSpeed;
        this.effectiveSpeedUnit = effectiveSpeedUnit;
        this.latLon = latLon;
        this.impactRadiusMtrs = impactRadiusMtrs;
    }

    public String getDescription() {
        return description;
    }

    public String getStartHHMM() {
        return startHHMM;
    }

    public String getEndHHMM() {
        return endHHMM;
    }

    public double getEffectiveSpeed() {
        return effectiveSpeed;
    }

    public String getEffectiveSpeedUnit() {
        return effectiveSpeedUnit;
    }

    public double[] getLatLon() {
        return latLon;
    }

    public double getImpactRadiusMtrs() {
        return impactRadiusMtrs;
    }

    @Override
    public String toString() {
        return (new Gson()).toJson(this);
    }
}
