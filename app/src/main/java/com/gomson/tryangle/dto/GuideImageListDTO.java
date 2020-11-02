package com.gomson.tryangle.dto;

import java.util.List;

public class GuideImageListDTO {

    private GuideDTO guideDTO;
    private List<String> guideImageList;

    public GuideImageListDTO() {
    }

    public GuideImageListDTO(GuideDTO guideDTO, List<String> guideImageList) {
        this.guideDTO = guideDTO;
        this.guideImageList = guideImageList;
    }

    public GuideDTO getGuideDTO() {
        return guideDTO;
    }

    public List<String> getGuideImageList() {
        return guideImageList;
    }
}
