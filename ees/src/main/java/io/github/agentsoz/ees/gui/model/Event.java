package io.github.agentsoz.ees.gui.model;

import java.time.LocalDateTime;

public class Event<V> {
    public LocalDateTime updated;
    public String summary;
    public Content<V> content = new Content<>();
}


