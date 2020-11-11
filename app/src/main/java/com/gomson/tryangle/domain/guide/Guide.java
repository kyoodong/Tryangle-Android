package com.gomson.tryangle.domain.guide;

import android.graphics.Color;

import com.gomson.tryangle.domain.component.Component;
import com.gomson.tryangle.view.LayerLayout;

import org.jetbrains.annotations.NotNull;

public class Guide {

    public static final int GREEN = Color.argb(200, 123, 225, 185);
    public static final int PINK = Color.argb(200, 255, 170, 170);
    public static final int TRANS_GREEN = Color.argb(50, 123, 225, 185);
    public static final int TRANS_PINK = Color.argb(50, 255, 170, 170);

    private Component component;
    private int guideId;
    private String message;

    public Guide(int guideId, String message, Component component) {
        this.guideId = guideId;
        this.message = message;
        this.component = component;
    }

    public int getGuideId() {
        return guideId;
    }

    public void setGuideId(int guideId) {
        this.guideId = guideId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    protected void guide(@NotNull LayerLayout layerLayout) {
        layerLayout.invalidate();
    }

    protected void clearGuide(@NotNull LayerLayout layerLayout) {
        layerLayout.invalidate();
    }
}
