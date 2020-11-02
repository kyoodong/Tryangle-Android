package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.Point;

public class ObjectGuide extends Guide {

    private Point diffPoint;

    public ObjectGuide(int guideId, Point diffPoint) {
        super(guideId);
        this.diffPoint = diffPoint;
    }

    public Point getDiffPoint() {
        return diffPoint;
    }
}
