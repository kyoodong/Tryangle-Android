package com.gomson.tryangle.domain.component;

import android.graphics.Bitmap;

import com.gomson.tryangle.Layer;
import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.pose.PoseClass;

import org.tensorflow.lite.examples.posenet.lib.Person;

public class PersonComponent extends ObjectComponent {

    private PoseClass pose;
    private Person person;

    public PersonComponent(long id, long componentId, int clazz, Point centerPoint, float area,
                           String mask, String roi, Bitmap roiImage, Layer layer, Person person, PoseClass pose) {
        super(id, componentId, clazz, centerPoint, area, mask, roi);
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
