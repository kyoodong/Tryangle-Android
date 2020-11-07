package com.gomson.tryangle.domain.guide;

abstract public class Guide {

    private int guideId;
    private String message;

    public Guide(int guideId, String message) {
        this.guideId = guideId;
        this.message = message;
    }

    public int getGuideId() {
        return guideId;
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public abstract void act();
}
