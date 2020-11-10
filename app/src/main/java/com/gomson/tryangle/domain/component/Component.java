package com.gomson.tryangle.domain.component;

import com.gomson.tryangle.domain.guide.Guide;

import java.util.ArrayList;

public abstract class Component {

    private long id;
    private long componentId;
    protected ArrayList<Guide> guideList;
    public boolean guideCompleted;

    public Component(long id, long componentId, ArrayList<Guide> guideList) {
        this.id = id;
        this.componentId = componentId;
        this.guideList = guideList;
    }

    public Component() {
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

    public void setGuideList(ArrayList<Guide> guideList) {
        this.guideList = guideList;
    }

    public ArrayList<Guide> getGuideList() {
        return guideList;
    }

    public Boolean getGuideCompleted() {
        return guideCompleted;
    }

    public void setGuideCompleted(Boolean guideCompleted) {
        this.guideCompleted = guideCompleted;
    }
}
