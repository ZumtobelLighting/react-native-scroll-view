package io.autodidact.zoomage;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

public class MatrixGestureDetector {
    private static final String TAG = "MatrixGestureDetector";

    private int ptpIdx = 0;
    private Matrix mTempMatrix = new Matrix();
    private Matrix mMatrix;
    private OnMatrixChangeListener mListener;
    private float[] mSrc = new float[4];
    private float[] mDst = new float[4];
    private int mCount;

    private float[] values = new float[9];

    private boolean mRotationEnabled;

    interface OnMatrixChangeListener {
        void onChange(Matrix matrix);
    }

    public MatrixGestureDetector(Matrix matrix, MatrixGestureDetector.OnMatrixChangeListener listener) {
        this.mMatrix = matrix;
        this.mListener = listener;
    }

    public void onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 2) {
            return;
        }

        int action = event.getActionMasked();
        int index = event.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                int idx = index * 2;
                mSrc[idx] = event.getX(index);
                mSrc[idx + 1] = event.getY(index);
                mCount++;
                ptpIdx = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < mCount; i++) {
                    idx = ptpIdx + i * 2;
                    mDst[idx] = event.getX(i);
                    mDst[idx + 1] = event.getY(i);
                }

                mTempMatrix.setPolyToPoly(mSrc, ptpIdx, mDst, ptpIdx, mCount);

                if(!mRotationEnabled){
                    mTempMatrix.getValues(values);
                    values[Matrix.MSKEW_X] = 0;
                    values[Matrix.MSKEW_Y] = 0;
                    mTempMatrix.setValues(values);
                }

                mMatrix.postConcat(mTempMatrix);

                if(mListener != null) {
                    mListener.onChange(mMatrix);
                }
                System.arraycopy(mDst, 0, mSrc, 0, mDst.length);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(index) == 0) ptpIdx = 2;
                mCount--;
                break;
        }
    }

    public MatrixGestureDetector setRotationEnabled(boolean enabled) {
        mRotationEnabled = enabled;
        return this;
    }
}