package com.gomson.tryangle.domain.component;

import android.graphics.Bitmap;

import com.gomson.tryangle.Layer;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class ObjectComponent extends Component {

    public static final int PERSON = 1;

    private int clazz;
    private int centerPointX;
    private int centerPointY;
    private float area;
    private String mask;
    private String roi;
    private ArrayList<ArrayList<Integer>> maskList;
    private ArrayList<Integer> roiList;

    private Bitmap roiImage;
    private Layer layer;

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
                    Object obj = arr.get(j);
                    if (obj instanceof Integer) {
                        this.maskList.get(i).add((Integer) obj);
                    } else {
                        Boolean b = (Boolean) obj;
                        this.maskList.get(i).add(b ? 1 : 0);
                    }
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

    public ArrayList<ArrayList<Integer>> getMaskList() {
        return maskList;
    }

    public ArrayList<Integer> getRoiList() {
        return roiList;
    }

    public Bitmap getRoiImage() {
        return roiImage;
    }

    public void setRoiImage(Bitmap roiImage) {
        this.roiImage = roiImage;
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    public void setCenterPointX(int centerPointX) {
        this.centerPointX = centerPointX;
    }

    public void setCenterPointY(int centerPointY) {
        this.centerPointY = centerPointY;
    }

    public void setArea(float area) {
        this.area = area;
    }

    public double getRoiArea() {
        int height = maskList.size();
        int width = maskList.get(0).size();
        int roiHeight = roiList.get(2) - roiList.get(0);
        int roiWidth = roiList.get(3) - roiList.get(1);

        return (double) (roiHeight * roiWidth) / (height * width);
    }

    private double normalize(double max, double normMax, double value) {
        return value / max * normMax;
    }

    @Override
    public double getPriority() {
        int height = maskList.size();
        int width = maskList.get(0).size();
        int centerX = width / 2;
        int centerY = height / 2;

        // 오브젝트 가이드가 먼저 이루어져야하므로 오브젝트 컴포넌트 가중치를 부여
        int objectClassScore = 100;

        // 오브젝트가 사진 내에서 차지하는 비율을 점수화
        double areaScore = area * 100 * 2;
        areaScore = Math.min(100, areaScore);

        // 중앙 지점에서의 유클리디언 거리를 이용하여 점수 측정
        double positionScore = 1000 - Math.sqrt(Math.pow(Math.abs(centerX - centerPointX), 2)
                + Math.pow(Math.abs(centerY - centerPointY), 2));
        positionScore = Math.max(0, positionScore);

        // 100점 만점으로 환산
        double score = normalize(100, 70, areaScore) + normalize(1000, 30, positionScore);

        // 오브젝트는 100점을 추가지급하여 100점부터 시작함
        return objectClassScore + score;
    }
}
