package io.autodidact.zoomablescrollview;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LayoutDirection;
import android.util.Log;
import android.view.ScaleGestureDetector;

public class MatrixManager extends Matrix implements IGestureDetector.ScaleHelper, IGestureDetector.TranslateHelper, IGestureDetector.MesaureTransformedView {
    private static final String TAG = RNZoomableScrollView.class.getSimpleName();

    protected RNZoomableScrollView mView;
    private Matrix mViewMatrix = new Matrix();

    private float mScale = 1f;
    private float minScale = 0.75f;
    private float maxScale = 3f;

    private boolean mAppliedChange;
    private boolean mDidInitScale = false;

    private RectB canOffset = new RectB();
    private MeasureTransformedView mMeasurementProvider;
    private boolean mCenterContent = false;

    /**
     * Common
     */

    MatrixManager(RNZoomableScrollView view){
        super();
        mMeasurementProvider = new MeasureTransformedView(view);
        mView = view;
    }

    private MatrixManager(MatrixManager ancestor){
        super();
        mMeasurementProvider = ancestor.mMeasurementProvider;
        mView = ancestor.mView;
        set(ancestor);
    }

    public MatrixManager test(){
        return new MatrixManager(this);
    }

    public MeasureTransformedView getMeasuringHelper() {
        return mMeasurementProvider;
    }

    public void processLayout(Rect clippingRect, Rect contentRect) {
        /*
        RectF clip;
        if(getMeasuringHelper().isInitialized()){
            clip = getMeasuringHelper().getClippingRect();
            preTranslate(-clip.left, -clip.top);
        }
        preTranslate(clippingRect.left, clippingRect.top);
        */
        Matrix src = new Matrix();
        mViewMatrix.invert(src);
        postConcat(src);
        mViewMatrix.set(mView.getMatrix());
        postConcat(mViewMatrix);

    }

    public void zoomToRect(float x, float y, float width, float height, boolean animated){
        RectF src = getAbsoluteLayoutRect();
        RectF dst = new RectF(x, y, x + width, y + height);
        reset();
        float scale = clampScale(Math.min(src.width() / width, src.height() / height));
        Log.d(TAG, "zoomToRect: " + src.width() / width + "   " + src.height() / height);
        //preTranslate();
        setScale(scale, scale, -x, -y);
    }

    public void forceUpdateFromMatrix() {
        float[] values = new float[9];
        getValues(values);
        mScale = clampScale(values[Matrix.MSCALE_X]);

        computeScroll();
    }

    public void zoomToRect(RectF dst, boolean animated) {
        RectF src = getClippingRect();
        src.offsetTo(0, 0);
        Matrix matrix = new Matrix();
        RectF post = new RectF();
        RectF pre = new RectF();

        //dst.offsetTo(-dst.left, -dst.top);
        matrix.setRectToRect(src, dst, ScaleToFit.START);
        mapRect(pre);
        reset();
        preTranslate(-src.left, -src.top);
        postConcat(matrix);
        mapRect(post);
        matrix.setRectToRect(pre, post, ScaleToFit.CENTER);
        forceUpdateFromMatrix();
    }

    public Matrix clampMatrix(Matrix src){
        return src;
    }

    public static float clamp(float min, float value, float max){
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Measuring Manager
     *
     */


    public boolean isContained(){
        return getTransformedRect().contains(getClippingRect());
    }

    public Matrix getRawViewMatrix(){
        Matrix out = new Matrix();
        //mView.setPivotX(0);
        //mView.setPivotY(0);
        out.preTranslate(-mView.getPivotX(), -mView.getPivotY());
        out.postConcat(mView.getMatrix());
        return out;
/*
        RectF mLayout = getAbsoluteContentRect();
        Matrix m = new Matrix();
        //m.preTranslate(mLayout.left, mLayout.top);

        Matrix m1 = new Matrix();
        RectF mapped = new RectF(mLayout);
        mapped.offsetTo(0, 0);
        mView.getMatrix().mapRect(mapped);
        Log.d(TAG, "getAbsoluteMatrix: " + mapped);
        m1.setRectToRect(mLayout, mapped, ScaleToFit.START);
        //m.postConcat(getRawViewMatrix());
        m.postConcat(m1);
        return m;
        */
    }

    public Matrix getAbsoluteMatrix(){
        Matrix m = new Matrix();
        m.postConcat(this);
        return m;
    }

    public RectF getAbsoluteLayoutRect(){
        return mMeasurementProvider.getAbsoluteContentRect();
    }

    @Override
    public RectF getClippingRect() {
        return mMeasurementProvider.getClippingRect();
    }

    public RectF getTransformedRect(){
        RectF src = new RectF(getAbsoluteLayoutRect());
        RectF dst = new RectF();
        src.offsetTo(0, 0);
        getAbsoluteMatrix().mapRect(dst, src);
        return dst;
    }


    /**
     * ScaleManager
     *
     */


    /*
        public void setInitialScale(float scale){
            if(mDidInitScale) return;
            mDidInitScale = true;
            mScale = clampScale(scale);
            postScale(mScale, mScale, 0, 0);
        }
    */
    public float getScale() {
        return mScale;
    }

    @Override
    public float getMinimumScale() {
        return minScale;
    }

    public void setMinimumScale(float minimumScale) {
        minScale = minimumScale;
    }

    @Override
    public float getMaximumScale() {
        return maxScale;
    }

    public void setMaximumScale(float maximumScale) {
        maxScale = maximumScale;
    }

    public void setCenterContent(boolean centerContent) {
        mCenterContent = centerContent;
    }

    @Override
    public float clampScale(float scale) {
        return clamp(getMinimumScale(), scale, getMaximumScale());
    }

    @Override
    public float clampScaleFactor(float currentScale, float scaleBy) {
        return clamp(getMinimumScale() / currentScale, scaleBy, getMaximumScale() / currentScale);
    }

    private float clampScaleFactor(float scaleBy){
        return clampScaleFactor(mScale, scaleBy);
    }

    public void setScale(float scale, float focusX, float focusY, boolean animated){
        postScale(scale / mScale, focusX, focusY);
    }

    public void postScale(ScaleGestureDetector detector){
        float scaleBy = clampScaleFactor(detector.getScaleFactor());
        PointF focal = getFocalPoint(detector.getFocusX(), detector.getFocusY());
        postScale(scaleBy, focal.x, focal.y);
    }

    public PointF getFocalPoint(float x, float y){
        PointF out = new PointF(x, y);
        if(!isContained()) {
            RectF clippingRect = getClippingRect();
            if(mCenterContent){
                out.set(clippingRect.centerX(), clippingRect.centerY());
            }
            else {
                out.set(mMeasurementProvider.getLayoutDirection() == LayoutDirection.RTL ? clippingRect.right : clippingRect.left, clippingRect.top);
            }
        }
        return out;
    }

    public void postScale(float scaleBy, float focusX, float focusY){
        float outScaleBy = clampScaleFactor(scaleBy);
        mScale *= outScaleBy;
        if(outScaleBy != 1) mAppliedChange = true;
        postScale(outScaleBy, outScaleBy, focusX, focusY);
    }

    public void tapToScale(float x, float y){
        float min = Math.max(1, minScale);
        float max = maxScale;
        float m = (min + max) / 2;
        float scaleTo = mScale > m ? max : min;
        setScale(scaleTo, x, y, true);
    }

    /*
     * TranslateManager
     *
     */

    @Override
    public PointF getTopLeftMaxDisplacement() {
        RectF o = getTransformedRect();
        RectF clippingRect = getClippingRect();
        //return new PointF(Math.min(o.left - clippingRect.left, 0), Math.min(o.top - clippingRect.top, 0));
        return new PointF(o.left - clippingRect.left, o.top - clippingRect.top);
    }

    @Override
    public PointF getTopLeftMaxDisplacement(PointF distance) {
        PointF p = getTopLeftMaxDisplacement();
        p.offset(-distance.x, -distance.y);
        return p;
    }

    @Override
    public PointF getBottomRightMaxDisplacement() {
        RectF o = getTransformedRect();
        RectF clippingRect = getClippingRect();
        //return new PointF(Math.max(o.right - clippingRect.right, 0), Math.max(o.bottom - clippingRect.bottom, 0));
        return new PointF(o.right - clippingRect.right, o.bottom - clippingRect.bottom);
    }

    @Override
    public PointF getBottomRightMaxDisplacement(PointF distance) {
        PointF p = getBottomRightMaxDisplacement();
        p.offset(-distance.x, -distance.y);
        return p;
    }

    @Override
    public boolean canScroll(PointF velocity) {
        boolean scrollX = velocity.x == 0 ? true : velocity.x > 0 ? canOffset.left : canOffset.right;
        boolean scrollY = velocity.y == 0 ? true : velocity.y > 0 ? canOffset.top : canOffset.bottom;

        return Math.abs(velocity.x) > Math.abs(velocity.y) ? scrollX : scrollY;
        /*
        return new RectB(
                mVelocity.x >= 0 && canOffset.left,
                mVelocity.y >= 0 && canOffset.top,
                mVelocity.x <= 0 && canOffset.right,
                mVelocity.y <= 0 && canOffset.bottom
        );
        */
    }

    @Override
    public void computeScroll() {
        clampOffset(new PointF(0, 0));
    }

    @Override
    public PointF clampOffset(PointF offset) {
        RectB mCanOffset = new RectB(false);
        PointF out =  new PointF();
        out.set(offset);
        RectF clippingRect = getClippingRect();
        RectF transformed = getTransformedRect();
        transformed.offset(offset.x, offset.y);

        boolean wIsContained = transformed.width() <= clippingRect.width();
        boolean hIsContained = transformed.height() <= clippingRect.height();

        if(transformed.left < clippingRect.left && !wIsContained) mCanOffset.left = true;
        if(transformed.right > clippingRect.right && !wIsContained) mCanOffset.right = true;
        if(transformed.top < clippingRect.top && !hIsContained) mCanOffset.top = true;
        if(transformed.bottom > clippingRect.bottom && !hIsContained) mCanOffset.bottom = true;

        if(wIsContained) out.x = 0;
        else if(transformed.left > clippingRect.left) out.offset(clippingRect.left - transformed.left, 0);
        else if(transformed.right < clippingRect.right) out.offset(clippingRect.right - transformed.right, 0);

        if(hIsContained) out.y = 0;
        else if(transformed.top > clippingRect.top) out.offset(0, clippingRect.top - transformed.top);
        else if(transformed.bottom < clippingRect.bottom) out.offset(0, clippingRect.bottom - transformed.bottom);

        canOffset.set(mCanOffset);
        return out;
    }

    public boolean isOverScrolling() {
        RectB mIsOverscrolling = new RectB(false);
        RectF clippingRect = getClippingRect();
        RectF transformed = getTransformedRect();

        boolean wIsContained = transformed.width() <= clippingRect.width();
        boolean hIsContained = transformed.height() <= clippingRect.height();

        if(transformed.left > clippingRect.left && !wIsContained) mIsOverscrolling.left = true;
        if(transformed.right > clippingRect.right && !wIsContained) mIsOverscrolling.right = true;
        if(transformed.top < clippingRect.top && !hIsContained) mIsOverscrolling.top = true;
        if(transformed.bottom < clippingRect.bottom && !hIsContained) mIsOverscrolling.bottom = true;

        return mIsOverscrolling.some();
    }

    public void fling(){
        /*
        mScroller.fling(
                getScrollX(), // startX
                getScrollY(), // startY
                0, // velocityX
                correctedVelocityY, // velocityY
                0, // minX
                0, // maxX
                0, // minY
                Integer.MAX_VALUE, // maxY
                0, // overX
                scrollWindowHeight / 2 // overY
        );
        */
    }

    public boolean scrollTo(PointF scrollTo) {
        RectF transformedRect = getTransformedRect();
        MatrixManager testMatrix = test();
        RectF out = new RectF();

        PointF start = getTopLeftMaxDisplacement();
        PointF clamped = new PointF(Math.min(-start.x, scrollTo.x), Math.min(-start.y, scrollTo.y));

        out.set(transformedRect);
        out.offsetTo(clamped.x, clamped.y);

        postTranslate(-start.x, -start.y);
        postTranslate(-out.left, -out.top);

        return clamped.equals(scrollTo);
    }

    public boolean scrollBy(PointF scrollBy) {
        PointF p = new PointF();
        p.set(scrollBy);
        p.negate();
        PointF out = clampOffset(p);
        postTranslate(out.x, out.y);
        return out.length() > 0;
    }

    public void scrollToEnd(boolean horizontal) {
        RectF layoutRect = getAbsoluteLayoutRect();
        RectF clippingRect = getClippingRect();
        layoutRect.inset(clippingRect.width() * 0.5f, clippingRect.height() * 0.5f);
        layoutRect.offsetTo(0, 0);
        Log.d(TAG, "scrollToEnd: " + layoutRect);
        scrollTo(new PointF(layoutRect.right, layoutRect.bottom));
        /*
        PointF p = getBottomRightMaxDisplacement();
        p.negate();
        if(horizontal){
            p.x = 0;
        }
        else {
            p.y = 0;
            if(mMeasurementProvider.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL){
                p.negate();
            }
        }
        scrollBy(p);
        */
    }
}