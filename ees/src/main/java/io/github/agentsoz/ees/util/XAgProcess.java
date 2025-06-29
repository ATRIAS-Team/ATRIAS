package io.github.agentsoz.ees.util;

import java.util.Map;

public class XAgProcess extends Data<XAgProcess> {
    private XAgProcess(Builder builder) {
        this.name = builder.name;
        this.trigger = builder.trigger;
        this.queries = builder.queries;
        this.criteria = builder.criteria;
        this.actions = builder.actions;
        this.notification = builder.notification;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String trigger;
        private Map<String, Object> queries;
        private String criteria;
        private Map<String, Object> actions;
        private String[] notification;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder trigger(String trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder queries(Map<String, Object> queries) {
            this.queries = queries;
            return this;
        }

        public Builder criterion(String criterion) {
            this.criteria = criterion;
            return this;
        }

        public Builder actions(Map<String, Object> actions) {
            this.actions = actions;
            return this;
        }

        public Builder notification(String... notification) {
            this.notification = notification;
            return this;
        }

        public XAgProcess build() {
            return new XAgProcess(this);
        }
    }

}

