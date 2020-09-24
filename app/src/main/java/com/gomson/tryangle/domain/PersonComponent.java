package com.gomson.tryangle.domain;

import lombok.Getter;

public class PersonComponent extends ObjectComponent {

    private int pose;

    public PersonComponent(long id, long componentId, int clazz, int centerPointX, int centerPointY, float area,
                           String mask, String roi, int pose) {
        super(id, componentId, clazz, centerPointX, centerPointY, area, mask, roi);
        this.pose = pose;
    }

    public int getPose() {
        return pose;
    }
}
