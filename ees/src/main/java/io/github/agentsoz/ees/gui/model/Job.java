package io.github.agentsoz.ees.gui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    String customerID;
    String jobID;
    LocalDateTime bookingTime;
    LocalDateTime vaTime;
    Location startPosition;
    Location endPosition;

}
