package com.gomson.tryangle.domain.guide;

import org.opencv.core.Point;

public class LineGuide extends Guide {

    private Point startPoint;
    private Point endPoint;

    public LineGuide(long objectId, int guideId, Point startPoint, Point endPoint) {
        super(objectId, guideId);

        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }
}
