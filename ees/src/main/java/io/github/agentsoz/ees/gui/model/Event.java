package io.github.agentsoz.ees.gui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event<V> {
    LocalDateTime updated;
    String summary;

    Content<V> content = new Content<>();
}


