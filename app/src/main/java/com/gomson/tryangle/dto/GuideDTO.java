package com.gomson.tryangle.dto;

import com.gomson.tryangle.domain.component.LineComponent;
import com.gomson.tryangle.domain.component.ObjectComponent;
import com.gomson.tryangle.domain.component.PersonComponent;

import java.util.List;

public class GuideDTO {

    private List<LineComponent> lineComponentList;
    private List<ObjectComponent> objectComponentList;
    private List<PersonComponent> personComponentList;
    private List<Integer> dominantColorList;

    public List<LineComponent> getLineComponentList() {
        return lineComponentList;
    }

    public List<ObjectComponent> getObjectComponentList() {
        return objectComponentList;
    }

    public List<PersonComponent> getPersonComponentList() {
        return personComponentList;
    }

    public List<Integer> getDominantColorList() {
        return dominantColorList;
    }
}
