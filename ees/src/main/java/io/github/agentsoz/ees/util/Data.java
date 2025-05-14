package io.github.agentsoz.ees.util;

import java.util.Map;

public abstract class Data<V> {
    String name;
    String trigger;
    Map<String, Object> queries;
    String criteria;
    Map<String, Object> actions;
    String[] notification;

    public String location;
    public String trace;
    public String belief;
    public V oldValue;
    public V newValue;
}
