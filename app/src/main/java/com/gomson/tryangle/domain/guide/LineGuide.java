package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.component.LineComponent;

public class LineGuide extends Guide {

    private Point startPoint;
    private Point endPoint;

    public LineGuide(LineComponent component, int guideId, Point startPoint, Point endPoint) {
        super(component, guideId);

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
    public LineComponent getComponent() {
        return (LineComponent) super.getComponent();
    }
}
