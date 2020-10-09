package com.gomson.tryangle.domain;

import android.graphics.Bitmap;

import com.gomson.tryangle.Layer;
import com.gomson.tryangle.pose.PoseClass;

import org.tensorflow.lite.examples.posenet.lib.Person;

public class PersonComponent extends ObjectComponent {

    private PoseClass pose;
    private Person person;

    public PersonComponent(long id, long componentId, int clazz, int centerPointX, int centerPointY, float area,
                           String mask, String roi, Bitmap roiImage, Layer layer, Person person, PoseClass pose) {
        super(id, componentId, clazz, centerPointX, centerPointY, area, mask, roi);
        setRoiImage(roiImage);
        setLayer(layer);
        this.person = person;
        this.pose = pose;
    }

    public PoseClass getPose() {
        return pose;
    }

    public Person getPerson() {
        return person;
    }
}
