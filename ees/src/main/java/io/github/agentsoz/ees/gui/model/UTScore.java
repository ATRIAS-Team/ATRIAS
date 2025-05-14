package io.github.agentsoz.ees.gui.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UTScore {
    String bidderID;
    LocalDateTime bidTime;
    Double score;
    String tag;
}
