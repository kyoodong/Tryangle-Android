package com.gomson.tryangle.domain;

import java.util.List;

public class Spot {

    private long id;
    private String name;
    private double x;
    private double y;
    private List<String> imageUrlList;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public List<String> getImageUrlList() {
        return imageUrlList;
    }
}
