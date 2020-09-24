package com.gomson.tryangle.dto;

import com.gomson.tryangle.domain.Component;
import com.gomson.tryangle.domain.Guide;

import java.util.List;

import lombok.Getter;

@Getter
public class GuideDTO {

    private List<List<Guide>> guideList;
    private List<Component> componentList;
    private List<Integer> dominantColorList;

}
