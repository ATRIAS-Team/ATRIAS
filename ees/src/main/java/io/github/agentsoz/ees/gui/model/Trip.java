package io.github.agentsoz.ees.gui.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    String tripID;

    Job job;

    DecisionTask decisionTask;

    String tripType; //charging trip, customer trip, ...

    LocalDateTime vaTime; // vehicle arriving time
    Location startPosition; // use this for trips with just one Geolocation
    Location endPosition ; // End of the trip used for customer trips
    String progress;

    LocalDateTime endTime;
}
