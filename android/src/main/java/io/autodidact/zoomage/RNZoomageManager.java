package io.autodidact.zoomage;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RNZoomageManager extends ViewGroupManager<ZoomageViewGroup> {
    @Nonnull
    @Override
    public String getName() {
        return "RNZoomageManager";
    }

    public RNZoomageManager(){
        super();
    }

    @Nonnull
    @Override
    protected ZoomageViewGroup createViewInstance(@Nonnull ThemedReactContext reactContext) {
        return new ZoomageViewGroup(reactContext);
    }

    @ReactProp(name = "minimumZoomScale", defaultFloat = 0.75f)
    public void setMinimumZoomScale(ZoomageViewGroup view, float value){
        view.getTransformHandler().setMinimunScale(value);
    }

    @ReactProp(name = "maximumZoomScale", defaultFloat = 3.0f)
    public void setMaximumZoomScale(ZoomageViewGroup view, @Nullable float value){
        view.getTransformHandler().setMaximunScale(value);
    }

    @Override
    public @Nullable
    Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                Event.EventNames.ON_SCALE,
                MapBuilder.of("registrationName", Event.EventNames.ON_SCALE),
                Event.EventNames.ON_SCALE_BEGIN,
                MapBuilder.of("registrationName", Event.EventNames.ON_SCALE_BEGIN),
                Event.EventNames.ON_SCALE_END,
                MapBuilder.of("registrationName", Event.EventNames.ON_SCALE_END)
        );
    }
}