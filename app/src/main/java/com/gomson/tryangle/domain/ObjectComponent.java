package com.gomson.tryangle.domain;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ObjectComponent extends Component {

    private int clazz;
    private int centerPointX;
    private int centerPointY;
    private float area;
    private String mask;
    private String roi;
    private List<List<Integer>> maskList;
    private List<Integer> roiList;

    public ObjectComponent(long id, long componentId, int clazz, int centerPointX, int centerPointY, float area,
                           String mask, String roi) {
        super(id, componentId);
        this.clazz = clazz;
        this.centerPointX = centerPointX;
        this.centerPointY = centerPointY;
        this.area = area;
        setMask(mask);
        setRoi(roi);
    }

    public void setMask(String mask) {
        this.mask = mask;

        try {
            this.maskList = new ArrayList<>();
            JSONArray array = new JSONArray(mask);
            for (int i = 0; i < array.length(); i++) {
                JSONArray arr = array.getJSONArray(i);
                this.maskList.add(new ArrayList<>());
                for (int j = 0; j < arr.length(); j++) {
                    this.maskList.get(i).add(arr.getInt(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setRoi(String roi) {
        this.roi = roi;

        try {
            this.roiList = new ArrayList<>();
            JSONArray array = new JSONArray(roi);
            for (int i = 0; i < array.length(); i++) {
                this.roiList.add(array.getInt(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getClazz() {
        return clazz;
    }

    public int getCenterPointX() {
        return centerPointX;
    }

    public int getCenterPointY() {
        return centerPointY;
    }

    public float getArea() {
        return area;
    }

    public String getMask() {
        return mask;
    }

    public String getRoi() {
        return roi;
    }

    public List<List<Integer>> getMaskList() {
        return maskList;
    }

    public List<Integer> getRoiList() {
        return roiList;
    }
}
