package com.gomson.tryangle.domain;

public class Component {

    private long id;
    private long componentId;

    public Component(long id, long componentId) {
        this.id = id;
        this.componentId = componentId;
    }

    public long getId() {
        return id;
    }

    public long getComponentId() {
        return componentId;
    }
}
