package com.gomson.tryangle.domain.component;

import android.graphics.Bitmap;
import android.util.Log;

import com.gomson.tryangle.Layer;
import com.gomson.tryangle.domain.Point;
import com.gomson.tryangle.domain.Roi;
import com.gomson.tryangle.domain.guide.Guide;
import com.gomson.tryangle.domain.guide.LineGuide;
import com.gomson.tryangle.domain.guide.ObjectGuide;
import com.gomson.tryangle.dto.MaskList;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class ObjectComponent extends Component {

    public static final int PERSON = 1;

    private int clazz;
    private Point centerPoint;
    private float area;
    private String roiStr;
    private MaskList mask;
    private Roi roi;

    private Bitmap roiImage;
    private Layer layer;

    public ObjectComponent() {
        guideList = new ArrayList<>();
        mask = new MaskList();
    }

    public ObjectComponent(long id, long componentId, ArrayList<ObjectGuide> guideList, int clazz,
                           Point centerPoint, float area,
                           MaskList mask, String roiStr) {
        super(id, componentId, guideList);
        this.clazz = clazz;
        this.centerPoint = centerPoint;
        this.area = area;
        setMask(mask);
        setRoiStr(roiStr);
    }

    public void setMask(MaskList mask) {
        this.mask = mask;
    }

    public void setRoiStr(String roiStr) {
        this.roiStr = roiStr;

        if (roiStr == null)
            return;

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

    public String getRoiStr() {
        return roiStr;
    }

    public MaskList getMask() {
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
        int width = mask.get(0).length;

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
        int width = mask.get(0).length;
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

    @Override
    public ArrayList<ObjectGuide> getGuideList() {
        ArrayList<ObjectGuide> guides = new ArrayList<>();
        for (Guide guide : guideList) {
            if (guide instanceof ObjectGuide) {
                guides.add((ObjectGuide) guide);
            } else {
                Log.d("dd", "dd");
            }
        }
        return guides;
    }

    public void refreshLayer(Bitmap bitmap) {
        layer = new Layer(mask, roi);
        centerPoint = layer.getCenterPoint();
        area = layer.getArea();
        roiImage = Bitmap.createBitmap(bitmap,
                roi.getLeft(), roi.getTop(),
                roi.getWidth(),
                roi.getHeight());
    }
}
