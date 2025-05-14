package io.github.agentsoz.ees.gui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Map;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Data<V> {
    public String name;
    public String trigger;
    public Map<String, Object> queries;
    public String criteria;
    public Map<String, Object> actions;
    public String[] notification;

    public String location;
    public String trace;
    public String belief;
    public V oldValue;
    public V newValue;
}
