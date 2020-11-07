package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.component.Component;
import com.gomson.tryangle.view.LayerLayout;

import org.jetbrains.annotations.NotNull;

public class LineGuide extends Guide {

    private Point startPoint;
    private Point endPoint;

    public LineGuide(int guideId, String message, Component component,
                     Point startPoint, Point endPoint) {
        super(guideId, message, component);

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
    public void guide(@NotNull LayerLayout layerLayout) {
        super.guide(layerLayout);
    }

    @Override
    public void clearGuide(@NotNull LayerLayout layerLayout) {
        super.clearGuide(layerLayout);
    }
}
