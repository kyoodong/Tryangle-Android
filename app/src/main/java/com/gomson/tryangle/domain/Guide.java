package com.gomson.tryangle.domain;

public class Guide {

    private long objectId;
    private int guideId;

    public Guide(long objectId, int guideId) {
        this.objectId = objectId;
        this.guideId = guideId;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public int getGuideId() {
        return guideId;
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
    }
}
