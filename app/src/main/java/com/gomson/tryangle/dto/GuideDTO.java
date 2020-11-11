package com.gomson.tryangle.dto;

import com.gomson.tryangle.domain.component.Component;
import com.gomson.tryangle.domain.component.LineComponent;
import com.gomson.tryangle.domain.component.ObjectComponent;
import com.gomson.tryangle.domain.component.PersonComponent;

import java.util.ArrayList;
import java.util.List;

public class GuideDTO {

    private List<LineComponent> lineComponentList;
    private List<ObjectComponent> objectComponentList;
    private List<PersonComponent> personComponentList;
    private List<Integer> dominantColorList;
    private MaskList mask;

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

    public void deployMask() {
        for (int i = 0; i < mask.size(); i++) {
            byte[] b = mask.get(i);

            for (ObjectComponent component : objectComponentList) {
                component.getMask().add(new byte[b.length]);
            }

            for (ObjectComponent component : personComponentList) {
                component.getMask().add(new byte[b.length]);
            }

            for (int j = 0; j < b.length; j++) {
                byte value = b[j];
                int c = 0;
                while (value > 0) {
                    ObjectComponent component = getObjectComponentByComponentId(c);
                    if (component == null)
                        continue;

                    if ((value & (1 << c)) > 0) {
                        component.getMask().get(i)[j] = 1;
                        value -= (1 << c);
                    } else {
                        component.getMask().get(i)[j] = 0;
                    }
                    c++;
                }
            }
        }
    }

    public void initPerson() {
        for (PersonComponent component : personComponentList) {
            component.initPerson();
        }
    }

    private ObjectComponent getObjectComponentByComponentId(long componentId) {
        for (ObjectComponent component : objectComponentList) {
            if (component.getComponentId() == componentId)
                return component;
        }

        for (ObjectComponent component : personComponentList) {
            if (component.getComponentId() == componentId)
                return component;
        }
        return null;
    }
}
