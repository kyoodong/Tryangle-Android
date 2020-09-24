package com.gomson.tryangle.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Component {

    private long id;
    private long componentId;

    public long getId() {
        return id;
    }

    public long getComponentId() {
        return componentId;
    }
}
