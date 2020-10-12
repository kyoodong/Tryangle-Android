package com.gomson.tryangle.domain.guide;

public class Guide {

    private long componentId;
    private int guideId;

    public Guide(long componentId, int guideId) {
        this.componentId = componentId;
        this.guideId = guideId;
    }

    public long getComponentId() {
        return componentId;
    }

    public void setComponentId(long componentId) {
        this.componentId = componentId;
    }

    public int getGuideId() {
        return guideId;
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
    }
}
