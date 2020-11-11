package com.gomson.tryangle.domain.component;

import android.graphics.Bitmap;

import com.gomson.tryangle.Layer;
import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.guide.Guide;
import com.gomson.tryangle.dto.MaskList;

import org.tensorflow.lite.examples.posenet.lib.BodyPart;
import org.tensorflow.lite.examples.posenet.lib.KeyPoint;
import org.tensorflow.lite.examples.posenet.lib.Person;
import org.tensorflow.lite.examples.posenet.lib.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersonComponent extends ObjectComponent {

    private int pose;
    private Map<String, Point> posePoints;
    private Person person;
    private Map<String, BodyPart> bodyPartMap = new HashMap<>();
    {
        bodyPartMap.put("NOSE", BodyPart.NOSE);
        bodyPartMap.put("LEFT_EYE", BodyPart.LEFT_EYE);
        bodyPartMap.put("RIGHT_EYE", BodyPart.RIGHT_EYE);
        bodyPartMap.put("LEFT_EAR", BodyPart.LEFT_EAR);
        bodyPartMap.put("RIGHT_EAR", BodyPart.RIGHT_EAR);
        bodyPartMap.put("LEFT_SHOULDER", BodyPart.LEFT_SHOULDER);
        bodyPartMap.put("RIGHT_SHOULDER", BodyPart.RIGHT_SHOULDER);
        bodyPartMap.put("LEFT_ELBOW", BodyPart.LEFT_ELBOW);
        bodyPartMap.put("RIGHT_ELBOW", BodyPart.RIGHT_ELBOW);
        bodyPartMap.put("LEFT_WRIST", BodyPart.LEFT_WRIST);
        bodyPartMap.put("RIGHT_WRIST", BodyPart.RIGHT_WRIST);
        bodyPartMap.put("LEFT_HIP", BodyPart.LEFT_HIP);
        bodyPartMap.put("RIGHT_HIP", BodyPart.RIGHT_HIP);
        bodyPartMap.put("LEFT_KNEE", BodyPart.LEFT_KNEE);
        bodyPartMap.put("RIGHT_KNEE", BodyPart.RIGHT_KNEE);
        bodyPartMap.put("LEFT_ANKLE", BodyPart.LEFT_ANKLE);
        bodyPartMap.put("RIGHT_ANKLE", BodyPart.RIGHT_ANKLE);
    }

    public PersonComponent() {
        super(0, 0, new ArrayList(), -1, new Point(-1, -1), -1, null, null);
        guideList = new ArrayList<>();
        setMask(new MaskList());
    }

    public PersonComponent(long id, long componentId, ArrayList<Guide> guideList, int clazz, Point centerPoint, float area,
                           MaskList mask, String roi, Bitmap roiImage, Layer layer, Person person, int pose) {
        super(id, componentId, guideList, clazz, centerPoint, area, mask, roi);
        setCroppedImage(roiImage);
        setLayer(layer);
        this.person = person;
        this.pose = pose;
    }

    public int getPose() {
        return pose;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public void setPosePoints(Map<String, Point> posePoints) {
        this.posePoints = posePoints;
    }

    public void initPerson() {
        Set<String> keySet = posePoints.keySet();

        person = new Person();
        List<KeyPoint> keyPointList = new ArrayList<>();

        for (String key : keySet) {
            Point p = posePoints.get(key);
            KeyPoint keyPoint = new KeyPoint();
            BodyPart bodyPart = bodyPartMap.get(key);
            keyPoint.setBodyPart(bodyPart);
            if (p.getX() < 0 || p.getY() < 0)
                keyPoint.setScore(0);
            else
                keyPoint.setScore(1);

            Position position = new Position();
            position.setX(p.getX());
            position.setY(p.getY());
            keyPoint.setPosition(position);
            keyPointList.add(keyPoint);
        }

        person.setKeyPoints(keyPointList);
    }
}
