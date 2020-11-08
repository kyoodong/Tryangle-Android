package com.gomson.tryangle.domain.guide;

import com.gomson.tryangle.domain.Line;
import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.Roi;
import com.gomson.tryangle.domain.component.Component;
import com.gomson.tryangle.view.LayerLayout;

import org.jetbrains.annotations.NotNull;

public class LineGuide extends Guide {

    private Point startPoint;
    private Point endPoint;
    private Line line;

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
        line = new Line(
                startPoint,
                endPoint,
                Guide.GREEN
        );
        layerLayout.getLineList().add(line);
        super.guide(layerLayout);
    }

    @Override
    public void clearGuide(@NotNull LayerLayout layerLayout) {
        layerLayout.getLineList().remove(line);
        line = null;
        super.clearGuide(layerLayout);
    }

    public boolean isMatch(@NotNull Line line) {
        return line.isClose(line);
    }
}
