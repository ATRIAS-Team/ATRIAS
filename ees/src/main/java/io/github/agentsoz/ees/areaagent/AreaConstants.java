package io.github.agentsoz.ees.areaagent;

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

import org.w3c.dom.Element;

import static io.github.agentsoz.ees.Run.XMLConfig.assignIfNotNull;

public class AreaConstants {
    public static int REQUEST_WAIT_TIME = 20000;

    public static int MIN_TRIKES = -1;

    public static int NEIGHBOURS_WAIT_TIME = 10000;

    public static String CSV_SOURCE = "ees/subsample_2.csv";

    public static void configure(Element classElement) {
        assignIfNotNull(classElement,"CSV_SOURCE", String::toString,
                value -> AreaConstants.CSV_SOURCE = value);
        assignIfNotNull(classElement,"REQUEST_WAIT_TIME", Integer::parseInt,
                value -> AreaConstants.REQUEST_WAIT_TIME = value);
        assignIfNotNull(classElement,"MIN_TRIKES", Integer::parseInt,
                value -> AreaConstants.MIN_TRIKES = value);
        assignIfNotNull(classElement,"NEIGHBOURS_WAIT_TIME", Integer::parseInt,
                value -> AreaConstants.NEIGHBOURS_WAIT_TIME = value);
    }
}
