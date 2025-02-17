package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

public class Event<V> {
    public String updated;
    public String summary;

    public Content<V> content = new Content<>();

    public static class Content<V> {
        public long eventNumber;
        public String eventType;

        public Data<V> data = new Data<>();
    }

    public static class Data<V>{
        public String location;
        public String trace;

        public V oldValue;
        public V newValue;
    }
}


