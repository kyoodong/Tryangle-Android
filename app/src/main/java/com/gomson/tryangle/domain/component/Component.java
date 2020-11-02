package com.gomson.tryangle.domain.component;

import com.gomson.tryangle.domain.guide.Guide;

import java.util.ArrayList;

public abstract class Component {

    private long id;
    private long componentId;
    protected ArrayList<? extends Guide> guideList;

    public Component(long id, long componentId, ArrayList<? extends Guide> guideList) {
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

    public ArrayList<? extends Guide> getGuideList() {
        return new ArrayList<Guide>(guideList);
    }

    public void setGuideList(ArrayList<? extends Guide> guideList) {
        this.guideList = guideList;
    }
}
