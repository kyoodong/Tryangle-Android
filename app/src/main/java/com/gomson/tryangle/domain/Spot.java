package com.gomson.tryangle.domain;

import java.util.List;

public class Spot {

    private long id;
    private String name;
    private double lat;
    private double lon;
    private List<String> imageUrlList;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public List<String> getImageUrlList() {
        return imageUrlList;
    }
}
