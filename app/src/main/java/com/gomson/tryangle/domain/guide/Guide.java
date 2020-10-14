package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.component.Component;

public class Guide {

    private Component component;
    private int guideId;

    public Guide(Component component, int guideId) {
        this.component = component;
        this.guideId = guideId;
    }

    public int getGuideId() {
        return guideId;
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
    }

    public Component getComponent() {
        return component;
    }
}
