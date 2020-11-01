package com.gomson.tryangle.domain.component;

import com.gomson.tryangle.domain.guide.Guide;

import java.util.ArrayList;

public abstract class Component {

    private long id;
    private long componentId;
    private ArrayList<Guide> guideList;

    public Component(long id, long componentId, ArrayList<Guide> guideList) {
        this.id = id;
        this.componentId = componentId;
        this.guideList = guideList;
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

    public ArrayList<Guide> getGuideList() {
        return guideList;
    }
}
