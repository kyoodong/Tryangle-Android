package com.gomson.tryangle.domain.component;

import android.graphics.Bitmap;

import com.gomson.tryangle.Layer;
import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.guide.Guide;
import com.gomson.tryangle.domain.guide.ObjectGuide;
import com.gomson.tryangle.dto.MaskList;
import com.gomson.tryangle.pose.PoseClass;

import org.tensorflow.lite.examples.posenet.lib.Person;

import java.util.ArrayList;

public class PersonComponent extends ObjectComponent {

    private PoseClass pose;
    private Person person;

    public PersonComponent() {
        super(0, 0, new ArrayList<ObjectGuide>(), -1, new Point(-1, -1), -1, null, null);
        guideList = new ArrayList<>();
        setMask(new MaskList());
    }

    public PersonComponent(long id, long componentId, ArrayList<ObjectGuide> guideList, int clazz, Point centerPoint, float area,
                           String mask, String roi, Bitmap roiImage, Layer layer, Person person, PoseClass pose) {
        super(id, componentId, guideList, clazz, centerPoint, area, mask, roi);
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
