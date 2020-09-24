package com.gomson.tryangle.domain;

public class ObjectGuide extends Guide {

    private int diffX;
    private int diffY;
    private int objectClass;

    public ObjectGuide(int objectId, int guideId, int diffX, int diffY, int objectClass) {
        super(objectId, guideId);
        this.diffX = diffX;
        this.diffY = diffY;
        this.objectClass = objectClass;
    }

    public int getDiffX() {
        return diffX;
    }

    public void setDiffX(int diffX) {
        this.diffX = diffX;
    }

    public int getDiffY() {
        return diffY;
    }

    public void setDiffY(int diffY) {
        this.diffY = diffY;
    }

    public int getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(int objectClass) {
        this.objectClass = objectClass;
    }
}
