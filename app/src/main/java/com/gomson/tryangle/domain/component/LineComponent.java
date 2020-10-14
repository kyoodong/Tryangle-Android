package com.gomson.tryangle.domain.component;

public class LineComponent extends Component {

    private int startX;
    private int startY;
    private int endX;
    private int endY;

    public LineComponent(long id, long componentId, int startX, int startY, int endX, int endY) {
        super(id, componentId);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    @Override
    public double getPriority() {
        double score = Math.abs(startX - endX) + Math.abs(startY - endY);
        score /= 100;
        return score;
    }
}
