package com.gomson.tryangle.domain.component;

import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.guide.Guide;
import com.gomson.tryangle.domain.guide.LineGuide;

import java.util.ArrayList;

public class LineComponent extends Component {

    private Point start;
    private Point end;

    public LineComponent(long id, long componentId, ArrayList<Guide> guideList, Point start, Point end) {
        super(id, componentId, guideList);
        this.start = start;
        this.end = end;
    }

    public LineComponent(long id, long componentId, Point start, Point end) {
        this(id, componentId, new ArrayList<>(), start, end);
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    @Override
    public double getPriority() {
        double score = Math.abs(start.getX() - end.getX()) + Math.abs(start.getY() - end.getY());
        score /= 100;
        return score;
    }
}
