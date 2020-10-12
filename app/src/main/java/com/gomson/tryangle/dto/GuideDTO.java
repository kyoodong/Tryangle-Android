package com.gomson.tryangle.dto;

import com.gomson.tryangle.domain.component.Component;
import com.gomson.tryangle.domain.guide.Guide;

import java.util.List;

public class GuideDTO {

    private List<List<Guide>> guideList;
    private List<Component> componentList;
    private List<Integer> dominantColorList;


    public List<List<Guide>> getGuideList() {
        return guideList;
    }

    public List<Component> getComponentList() {
        return componentList;
    }

    public List<Integer> getDominantColorList() {
        return dominantColorList;
    }
}
