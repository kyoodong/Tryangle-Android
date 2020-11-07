package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.Point;

public class LineGuide extends Guide {

    private Point startPoint;
    private Point endPoint;

    public LineGuide(int guideId, Point startPoint, Point endPoint) {
        super(guideId);

        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    @Override
    public void act() {

    }
}
