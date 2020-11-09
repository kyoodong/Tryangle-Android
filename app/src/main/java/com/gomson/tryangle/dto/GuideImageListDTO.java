package com.gomson.tryangle.dto;

import java.util.ArrayList;

public class GuideImageListDTO {

    private GuideDTO guideDTO;
    private ArrayList<String> guideImageList;

    public GuideImageListDTO() {
    }

    public GuideImageListDTO(GuideDTO guideDTO, ArrayList<String> guideImageList) {
        this.guideDTO = guideDTO;
        this.guideImageList = guideImageList;
    }

    public GuideDTO getGuideDTO() {
        return guideDTO;
    }

    public ArrayList<String> getGuideImageList() {
        return guideImageList;
    }
}
