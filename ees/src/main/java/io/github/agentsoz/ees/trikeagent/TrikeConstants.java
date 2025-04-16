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
    public static double DRIVING_SPEED = 40.0;
    public static boolean CNP_ACTIVE = true;
    public static double THETA = 1000.0; //allowed waiting time for customers.
    public static boolean ALLOW_CUSTOMER_MISS = true; // customer will miss when delay > THETA
    public static double DISTANCE_FACTOR = 3.0; //multiply with distance estimations for energyconsumption, to avoid an underestimation
    public static double CHARGING_THRESHOLD = 0.4; // Threshold to determine when a ChargingTrip should be generated

    public static double REQUEST_WAIT_TIME = 10000;

    public static int MIN_CNP_TRIKES = 8;

    public static int MAX_CNP_TRIKES = Integer.MAX_VALUE;

    public static void configure(Element classElement) {
        assignIfNotNull(classElement, "MIN_CNP_TRIKES", Integer::parseInt,
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

        assignIfNotNull(classElement, "MAX_CNP_TRIKES", Integer::parseInt,
                value -> TrikeConstants.MAX_CNP_TRIKES = value);
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

            new Location("", 330102.854, 4691520.662),
            new Location("", 328857.087, 4690902.686),
            new Location("", 330172.808, 4690320.889),
            new Location("", 330849.426, 4692559.547),
            new Location("", 330865.289, 4691459.978),
            new Location("", 329151.760, 4689062.867),

            //  GENERATED STATIONS
            new Location("", 324762.9435851084, 4688760.287864451),
            new Location("", 325949.09213241993, 4686642.252031564),
            new Location("", 322217.342077784, 4688819.858038509),
            new Location("", 323403.83414151863, 4686701.743788378),
            new Location("", 326122.33024411125, 4690819.215137012),
            new Location("", 327308.40650970844, 4688700.724986501),
            new Location("", 323576.523920245, 4690878.856450236),
            new Location("", 323749.36487197084, 4695057.792548527),
            new Location("", 324935.9829170297, 4692938.237747405),
            new Location("", 321202.87261179544, 4695117.590805509),
            new Location("", 322389.8334811698, 4692997.957520323),
            new Location("", 325109.1729510805, 4697118.00982595),
            new Location("", 326295.718663532, 4694998.001605847),
            new Location("", 322562.4764530094, 4697177.879270603),
            new Location("", 318484.4964299401, 4690998.160985326),
            new Location("", 319671.6023834492, 4688879.435509727),
            new Location("", 315938.2760554375, 4691057.824209307),
            new Location("", 317125.7248979454, 4688939.020279167),
            new Location("", 319843.5458025732, 4693057.684602189),
            new Location("", 321030.57918355206, 4690938.505065991),
            new Location("", 317297.1202773517, 4693117.418994059),
            new Location("", 319499.8114715646, 4684703.011165803),
            new Location("", 320686.4485152383, 4682585.806518205),
            new Location("", 316954.4828904163, 4684762.4463860085),
            new Location("", 318141.4635143094, 4682645.163350944),
            new Location("", 320858.4377937141, 4686761.242835936),
            new Location("", 322045.0023171414, 4684643.58323203),
            new Location("", 318312.9034846579, 4686820.749175296),
            new Location("", 325776.0044053674, 4682467.1146916915),
            new Location("", 326961.6819208671, 4680350.6003793655),
            new Location("", 323231.29561071674, 4682526.4569654735),
            new Location("", 324417.31733200396, 4680409.864303115),
            new Location("", 327134.9692194198, 4684524.749219557),
            new Location("", 328320.5745039295, 4682407.77969581),
            new Location("", 324590.0550315775, 4684584.162583633),
            new Location("", 331039.191461541, 4686523.290385925),
            new Location("", 332224.38043411274, 4684405.94434013),
            new Location("", 328494.21137089795, 4686582.767564431),
            new Location("", 329679.7444853363, 4684465.343138739),
            new Location("", 332398.9150284513, 4688581.621114685),
            new Location("", 333584.0320090645, 4686463.82049499),
            new Location("", 329853.73045598203, 4688641.169403599),
            new Location("", 330027.8654767546, 4692818.820124168),
            new Location("", 331213.5260703205, 4690699.954413901),
            new Location("", 327481.9937141838, 4692878.525282374),
            new Location("", 328667.99775936047, 4690759.581125251),
            new Location("", 331388.00925672, 4694878.441659167),
            new Location("", 332573.5978090006, 4692759.1222717315),
            new Location("", 328841.93359042634, 4694938.217976413),
            new Location("", 329016.0187845129, 4699118.675707679),
            new Location("", 330202.1494674207, 4696998.2928945655),
            new Location("", 326469.25731306965, 4699178.609027501),
            new Location("", 327655.7307545929, 4697058.147700956),
            new Location("", 330376.5823470503, 4701179.585300525),
            new Location("", 331562.64093918167, 4699058.749711816),
            new Location("", 327829.6175519853, 4701239.589827652),
            new Location("", 322735.2709183089, 4701359.620874073),
            new Location("", 323922.3569210648, 4699238.549672339),
            new Location("", 320187.88987260393, 4701419.6473954925),
            new Location("", 321375.31800480915, 4699298.497643256),
            new Location("", 324095.49999118375, 4703421.125402382),
            new Location("", 325282.5136102715, 4701299.601685147),
            new Location("", 321547.9152882163, 4703481.223159595),
            new Location("", 317468.66895831155, 4697297.640123135),
            new Location("", 318656.24227919127, 4695177.396377856),
            new Location("", 314921.5587545383, 4697357.531533137),
            new Location("", 316109.47427044256, 4695237.209266629),
            new Location("", 318828.140960744, 4699358.452941308),
            new Location("", 320015.6416566295, 4697237.756035978),
            new Location("", 316280.8261854325, 4699418.415567562),
            new Location("", 312203.85727201216, 4693236.909712045),
            new Location("", 313391.9184561869, 4691117.4947389895),
            new Location("", 309657.02058468293, 4693296.666040288),
            new Location("", 310845.424028456, 4691177.172575433),
            new Location("", 313562.56898196077, 4695297.029472883),
            new Location("", 314750.5573017204, 4693177.160696992),
            new Location("", 311015.5268102755, 4695356.856997683),
            new Location("", 313221.42256593594, 4686939.783733636),
            new Location("", 314409.0169693837, 4684821.888893705),
            new Location("", 310675.4767481468, 4686999.311954737),
            new Location("", 311863.41410426813, 4684881.3386899475),
            new Location("", 314579.71001722006, 4688998.612347887),
            new Location("", 315767.2316101043, 4686880.262807507),
            new Location("", 312033.5581373443, 4689058.211716945),
            new Location("", 314238.4782258435, 4680646.992733935),
            new Location("", 315425.6043036763, 4678530.620576158),
            new Location("", 311693.42523843463, 4680706.2930308785),
            new Location("", 312880.89495927544, 4678589.842515562),
            new Location("", 315596.34100342565, 4682704.527464746),
            new Location("", 316783.39432438376, 4680587.699713742),
            new Location("", 313051.0813781997, 4682763.898860673),
            new Location("", 320514.61142035696, 4678412.198507257),
            new Location("", 321700.7780177139, 4676296.517230693),
            new Location("", 317970.1765891525, 4678471.405907071),
            new Location("", 319156.6874552085, 4676355.646310499),
            new Location("", 321872.81427282543, 4680469.135499391),
            new Location("", 323058.90840206004, 4678352.998375653),
            new Location("", 319328.1731385167, 4680528.4139692485),
            new Location("", 326788.5446908884, 4676178.28085538),
            new Location("", 327973.7496465089, 4674063.2906006705),
            new Location("", 324244.7305612477, 4676237.395412672),
            new Location("", 325430.28040927736, 4674122.32687552),
            new Location("", 328147.0872366406, 4678234.619912859),
            new Location("", 329332.22001182823, 4676119.173557764),
            new Location("", 325603.0671391542, 4678293.805511205),
            new Location("", 332049.99410748883, 4680232.094345228),
            new Location("", 333234.70993341296, 4678116.270510252),
            new Location("", 329505.9076443468, 4680291.343727089),
            new Location("", 330690.9682996464, 4678175.441579559),
            new Location("", 333409.29703234544, 4682289.1315335035),
            new Location("", 334593.94091547624, 4680172.852232728),
            new Location("", 330865.0055112619, 4682348.451976763),
            new Location("", 337313.23279997736, 4684287.168585302),
            new Location("", 338497.46003627, 4682170.512470111),
            new Location("", 334768.87667065137, 4684346.55282267),
            new Location("", 335953.44867227424, 4682229.81836497),
            new Location("", 338673.29289419996, 4686344.902571604),
            new Location("", 339857.4484272385, 4684227.791626973),
            new Location("", 336128.7326183018, 4686404.357890569),
            new Location("", 336304.1634972661, 4690580.722888195),
            new Location("", 337488.8644706502, 4688462.546414601),
            new Location("", 333758.9147814274, 4690640.335001899),
            new Location("", 334943.9598317543, 4688522.080118706),
            new Location("", 337664.64260013937, 4692639.748479931),
            new Location("", 338849.2718225083, 4690521.118071724),
            new Location("", 335119.1903152965, 4692699.431724004),
            new Location("", 335294.56803709606, 4696878.605233349),
            new Location("", 336479.7412243361, 4694758.910956991),
            new Location("", 332748.42869355145, 4696938.445405718),
            new Location("", 333933.9452665886, 4694818.672653043),
            new Location("", 336655.4657139884, 4698938.9196877135),
            new Location("", 337840.56710227823, 4696818.772376406),
            new Location("", 334109.1233809945, 4698998.8310388485),



            //  TEMP: goethe stations
            new Location("", 476142.33,5553197.70),
            new Location("", 476172.65,5552839.64),
            new Location("", 476482.10,5552799.06),
            new Location("", 476659.13,5553054.12),
            new Location("", 476787.10,5552696.95),
            new Location("", 476689.45,5552473.11),
            new Location("", 476405.41,5552489.17),
            new Location("", 476100.86,5552372.79)
    );
}
