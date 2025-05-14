package io.github.agentsoz.ees.gui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content<V> {
    long eventNumber;
    String eventType;

    Data<V> data;
}