package io.github.agentsoz.ees.gui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    String name;
    double x;
    double y;
}
