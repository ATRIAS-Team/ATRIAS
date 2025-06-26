package io.github.agentsoz.ees.util;

import java.util.Map;

public abstract class Data<V> {
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
