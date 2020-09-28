package com.gomson.tryangle.domain;

public class Guide {

    private int objectId;
    private int guideId;

    public Guide(int objectId, int guideId) {
        this.objectId = objectId;
        this.guideId = guideId;
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    public int getGuideId() {
        return guideId;
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
    }
}
