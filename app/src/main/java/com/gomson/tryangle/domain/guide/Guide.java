package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.component.Component;

public class Guide {

    private Component targetComponent;
    private int guideId;

    public Guide(Component targetComponent, int guideId) {
        this.targetComponent = targetComponent;
        this.guideId = guideId;
    }

    public int getGuideId() {
        return guideId;
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
    }

    public Component getTargetComponent() {
        return targetComponent;
    }
}
