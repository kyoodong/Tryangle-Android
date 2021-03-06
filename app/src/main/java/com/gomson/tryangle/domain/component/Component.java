package com.gomson.tryangle.domain.component;

import com.gomson.tryangle.domain.guide.Guide;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public abstract class Component {

    private long id;
    private long componentId;
    protected ArrayList<Guide> guideList;
    public boolean guideCompleted;
    public boolean standardGuideCompleted;

    public Component(long id, long componentId, @NotNull ArrayList<Guide> guideList) {
        this.id = id;
        this.componentId = componentId;
        this.guideList = guideList;
    }

    public Component() {
        guideList = new ArrayList<>();
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

    public @NotNull ArrayList<Guide> getGuideList() {
        return guideList;
    }

    public Boolean getGuideCompleted() {
        return guideCompleted;
    }

    public void setGuideCompleted(Boolean guideCompleted) {
        this.guideCompleted = guideCompleted;
    }

    public boolean isStandardGuideCompleted() {
        return standardGuideCompleted;
    }

    public void setStandardGuideCompleted(boolean standardGuideCompleted) {
        this.standardGuideCompleted = standardGuideCompleted;
    }
}
