package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.component.ObjectComponent;

public class ObjectGuide extends Guide {

    private Point diffPoint;

    public ObjectGuide(ObjectComponent component, int guideId, Point diffPoint) {
        super(component, guideId);
        this.diffPoint = diffPoint;
    }

    public Point getDiffPoint() {
        return diffPoint;
    }

    @Override
    public ObjectComponent getTargetComponent() {
        return (ObjectComponent) super.getTargetComponent();
    }
}
