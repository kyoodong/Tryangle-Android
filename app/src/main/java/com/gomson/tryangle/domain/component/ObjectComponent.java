package com.gomson.tryangle.domain.component;

import android.graphics.Bitmap;

import com.gomson.tryangle.Layer;
import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.Roi;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class ObjectComponent extends Component {

    public static final int PERSON = 1;

    private int clazz;
    private Point centerPoint;
    private float area;
    private String maskStr;
    private String roiStr;
    private ArrayList<ArrayList<Integer>> mask;
    private Roi roi;

    private Bitmap roiImage;
    private Layer layer;

    public ObjectComponent(long id, long componentId, int clazz, Point centerPoint, float area,
                           String maskStr, String roiStr) {
        super(id, componentId);
        this.clazz = clazz;
        this.centerPoint = centerPoint;
        this.area = area;
        setMaskStr(maskStr);
        setRoiStr(roiStr);
    }

    public void setMaskStr(String maskStr) {
        this.maskStr = maskStr;

        try {
            this.mask = new ArrayList<>();
            JSONArray array = new JSONArray(maskStr);
            for (int i = 0; i < array.length(); i++) {
                JSONArray arr = array.getJSONArray(i);
                this.mask.add(new ArrayList<>());
                for (int j = 0; j < arr.length(); j++) {
                    Object obj = arr.get(j);
                    if (obj instanceof Integer) {
                        this.mask.get(i).add((Integer) obj);
                    } else {
                        Boolean b = (Boolean) obj;
                        this.mask.get(i).add(b ? 1 : 0);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setRoiStr(String roiStr) {
        this.roiStr = roiStr;

        try {
            JSONArray array = new JSONArray(roiStr);
            this.roi = new Roi(array.getInt(1), array.getInt(3), array.getInt(0), array.getInt(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getClazz() {
        return clazz;
    }

    public Point getCenterPoint() {
        return centerPoint;
    }

    public float getArea() {
        return area;
    }

    public String getMaskStr() {
        return maskStr;
    }

    public String getRoiStr() {
        return roiStr;
    }

    public ArrayList<ArrayList<Integer>> getMask() {
        return mask;
    }

    public Roi getRoi() {
        return roi;
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

    public void setArea(float area) {
        this.area = area;
    }

    public double getRoiArea() {
        int height = mask.size();
        int width = mask.get(0).size();

        return (double) (roi.getHeight() * roi.getWidth()) / (height * width);
    }

    public void setCenterPoint(Point centerPoint) {
        this.centerPoint = centerPoint;
    }

    private double normalize(double max, double normMax, double value) {
        return value / max * normMax;
    }

    @Override
    public double getPriority() {
        int height = mask.size();
        int width = mask.get(0).size();
        int centerX = width / 2;
        int centerY = height / 2;

        // 오브젝트 가이드가 먼저 이루어져야하므로 오브젝트 컴포넌트 가중치를 부여
        int objectClassScore = 100;

        // 오브젝트가 사진 내에서 차지하는 비율을 점수화
        double areaScore = area * 100 * 2;
        areaScore = Math.min(100, areaScore);

        // 중앙 지점에서의 유클리디언 거리를 이용하여 점수 측정
        double positionScore = 1000 - Math.sqrt(Math.pow(Math.abs(centerX - centerPoint.getX()), 2)
                + Math.pow(Math.abs(centerY - centerPoint.getY()), 2));
        positionScore = Math.max(0, positionScore);

        // 100점 만점으로 환산
        double score = normalize(100, 70, areaScore) + normalize(1000, 30, positionScore);

        // 오브젝트는 100점을 추가지급하여 100점부터 시작함
        return objectClassScore + score;
    }
}
