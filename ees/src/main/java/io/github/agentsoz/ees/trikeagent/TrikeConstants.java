package io.github.agentsoz.ees.trikeagent;

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
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;

import static io.github.agentsoz.ees.Run.XMLConfig.assignIfNotNull;


public class  TrikeConstants {
    public static int ASK_FOR_TRIKES_WAIT_TIME = 5000;

    public static int MANAGER_WAIT_TIME = 25000;

    public static int CONFIRM_WAIT_TIME = 5000;

    public static int PROPOSALS_WAIT_TIME = 5000;

    public static double commitThreshold = 50.0;
    public static double DRIVING_SPEED = 6.0;
    public static boolean CNP_ACTIVE = true;
    public static double THETA = 1000.0; //allowed waiting time for customers.
    public static boolean ALLOW_CUSTOMER_MISS = true; // customer will miss when delay > THETA
    public static double DISTANCE_FACTOR = 3.0; //multiply with distance estimations for energyconsumption, to avoid an underestimation
    public static double CHARGING_THRESHOLD = 0.4; // Threshold to determine when a ChargingTrip should be generated

    public static double REQUEST_WAIT_TIME = 10000;

    public static long MIN_CNP_TRIKES = 8;

    public static void configure(Element classElement) {
        assignIfNotNull(classElement, "MIN_CNP_TRIKES", Long::parseLong,
                value -> TrikeConstants.MIN_CNP_TRIKES = value);

        assignIfNotNull(classElement, "REQUEST_WAIT_TIME", Long::parseLong,
                value -> TrikeConstants.REQUEST_WAIT_TIME = value);

        assignIfNotNull(classElement, "commitThreshold", Double::parseDouble,
                value -> TrikeConstants.commitThreshold = value);

        assignIfNotNull(classElement, "DRIVING_SPEED", Double::parseDouble,
                value -> TrikeConstants.DRIVING_SPEED = value);

        assignIfNotNull(classElement, "CNP_ACTIVE", Boolean::parseBoolean,
                value -> CNP_ACTIVE = value);

        assignIfNotNull(classElement, "THETA", Double::parseDouble,
                value -> TrikeConstants.THETA = value);

        assignIfNotNull(classElement, "ALLOW_CUSTOMER_MISS", Boolean::parseBoolean,
                value -> TrikeConstants.ALLOW_CUSTOMER_MISS = value);

        assignIfNotNull(classElement, "DISTANCE_FACTOR", Double::parseDouble,
                value -> DISTANCE_FACTOR = value);

        assignIfNotNull(classElement, "CHARGING_THRESHOLD", Double::parseDouble,
                value -> TrikeConstants.CHARGING_THRESHOLD = value);

        assignIfNotNull(classElement, "ASK_FOR_TRIKES_WAIT_TIME", Integer::parseInt,
                value -> TrikeConstants.ASK_FOR_TRIKES_WAIT_TIME = value);

        assignIfNotNull(classElement, "MANAGER_WAIT_TIME", Integer::parseInt,
                value -> TrikeConstants.MANAGER_WAIT_TIME = value);

        assignIfNotNull(classElement, "CONFIRM_WAIT_TIME", Integer::parseInt,
                value -> TrikeConstants.CONFIRM_WAIT_TIME = value);

        assignIfNotNull(classElement, "PROPOSALS_WAIT_TIME", Integer::parseInt,
                value -> TrikeConstants.PROPOSALS_WAIT_TIME = value);
    }

    //Boston station
    public static List<Location> CHARGING_STATION_LIST = Arrays.asList(
            new Location("", 330129.503, 4690968.364),
            new Location("", 329969.702, 4690968.243),
            new Location("", 330226.178, 4690808.802),
            new Location("", 330394.635, 4691190.930),
            new Location("", 330019.125, 4690675.274),
            new Location("", 329830.824, 4690748.290),
            new Location("", 330061.408, 4691249.231),
            new Location("", 330085.391, 4690790.884),
            new Location("", 330376.760, 4691775.234),
            new Location("", 329510.461, 4690211.875),
            new Location("", 329947.761, 4690811.336),
            new Location("", 330734.09049771406, 4686338.209947874),
            new Location("", 331315.3926859213, 4686003.14675877),
            new Location("", 330459.654522083, 4686797.65147697),
            new Location("", 330522.2126332774, 4686923.635164486),
            new Location("", 330404.0268183075, 4685725.674994842),
            new Location("", 330267.0424466077, 4685598.519795385),
            new Location("", 330162.4159192683, 4685455.758060012),
            new Location("", 330934.30093666096, 4684885.481330267),
            new Location("", 331067.9425282917, 4684873.356880021),
            new Location("", 331458.9572182546, 4683871.1501955),
            new Location("", 331525.34854709206, 4683418.775171504),
            new Location("", 327719.550770866, 4685531.510325233),
            new Location("", 327479.17367278016, 4686069.85136981),
            new Location("", 326790.8477667136, 4689528.776679865),
            new Location("", 326567.774762258, 4689664.697967572),
            new Location("", 327710.277554634, 4689368.639028133),
            new Location("", 327973.19664333365, 4689270.511257319),
            new Location("", 328133.97907932295, 4689478.039451958),
            new Location("", 328190.7759946394, 4689772.728762476),
            new Location("", 326281.4664791448, 4689675.283094306),
            new Location("", 328265.46399824356, 4690081.059870234),
            new Location("", 328040.2401779549, 4690128.885234285),
            new Location("", 327987.49280368455, 4690429.764286722),
            new Location("", 328461.5517205638, 4690143.2098028455),
            new Location("", 328831.32978277106, 4690507.727442217),
            new Location("", 329767.2607187112, 4690445.978121304),
            new Location("", 330225.55006005947, 4690092.84661864),
            new Location("", 330912.8591505459, 4689999.019060219),
            new Location("", 331006.74102454877, 4690518.329129274),
            new Location("", 330983.7125710095, 4690296.8628525175),
            new Location("", 326752.3580612396, 4692458.707911453),
            new Location("", 326496.2080125784, 4692683.370884217),
            new Location("", 329044.6136624153, 4693086.086950928),
            new Location("", 329672.0546194103, 4692140.789613456),
            new Location("", 330351.5084426627, 4692166.455323417),
            new Location("", 331075.2075612657, 4693423.9441094035),
            new Location("", 332053.42006065475, 4693016.88156659),
            new Location("", 332260.02298745513, 4692919.660061572),
            new Location("", 332283.69022679795, 4692945.753122068),
            new Location("", 330773.91964123806, 4691695.911588468),
            new Location("", 330541.70079147693, 4692117.668885517),
            new Location("", 331125.97199872, 4692204.9227484055),
            new Location("", 329529.3681031796, 4687436.116361706),
            new Location("", 327010.8782558239, 4687482.809660871),
            new Location("", 327044.18152553355, 4687003.280819914),
            new Location("", 327065.7802733871, 4687885.381320049),
            new Location("", 328175.1632187184, 4688539.366192844),
            new Location("", 329529.4398850673, 4688287.695682363),
            new Location("", 329364.698601429, 4688423.9164145),
            new Location("", 330077.15229866316, 4687961.080699825),
            new Location("", 329202.0219202008, 4683190.730182893),
            new Location("", 328456.1722722984, 4682856.789983864),
            new Location("", 327991.48788313573, 4683102.576120801),
            new Location("", 328556.58158344286, 4683689.950927951),
            new Location("", 329198.0303689931, 4684123.306078233),
            new Location("", 329952.6582628207, 4684269.552173864),
            new Location("", 328692.95462842024, 4684355.114709307),
            new Location("", 328693.0153561778, 4684357.607604435),
            new Location("", 327943.6362818504, 4684632.793215642),
            new Location("", 328678.7676101875, 4684594.920629536),
            new Location("", 329081.35585095023, 4685113.209289537),
            new Location("", 328699.8707663481, 4685580.953568669),
            new Location("", 328600.20644835127, 4685703.284318363),

            new Location("", 331422.333, 4701643.240),
            new Location("", 317542.818, 4702442.737),
            new Location("", 306940, 4689177),
            new Location("", 315324.536, 4673956.624),
            new Location("", 332951.931, 4670915.936),
            new Location("", 340547.319, 4685536.832)
    );
    //Goethe stations
    /**
     public List<Location> CHARGING_STATION_LIST = Arrays.asList(
     new Location("", 476142.33,5553197.70),
     new Location("", 476172.65,5552839.64),
     new Location("", 476482.10,5552799.06),
     new Location("", 476659.13,5553054.12),
     new Location("", 476787.10,5552696.95),
     new Location("", 476689.45,5552473.11),
     new Location("", 476405.41,5552489.17),
     new Location("", 476100.86,5552372.79)
     );
     */

}
