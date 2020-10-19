package com.gomson.tryangle.domain.component;

public abstract class Component {

    private long id;
    private long componentId;

    public Component(long id, long componentId) {
        this.id = id;
        this.componentId = componentId;
    }

    public abstract double getPriority();

    public long getId() {
        return id;
    }

    public long getComponentId() {
        return componentId;
    }

    void setComponentId(long componentId) {
        this.componentId = componentId;
    }
}
