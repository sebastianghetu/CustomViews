package net.ghetu.customviews;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by Sebastian on 10/03/2016.
 */
public class MapPointsView extends ImageView {
    private static final String TAG = "MapPointsView";

    ArrayList<AnimatedPoint> mClickedPoints;
    ArrayList<AnimatedLine> mLines;

    int mMaxPoints = 5;

    public MapPointsView(Context context) {
        super(context);
        init();
    }

    public MapPointsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapPointsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean needsInvalidate = false;

        for (AnimatedLine l : mLines) {
            canvas.drawLine(l.getOrigin().X, l.getOrigin().Y, l.getCurrentPoint().X, l.getCurrentPoint().Y, l.getPaint());

            if (l.getAnimator().isRunning())
                needsInvalidate = true;
        }

        for (AnimatedPoint p : mClickedPoints) {
            // Draw the fixed points
            canvas.drawCircle(p.X, p.Y, p.getInitialDiameter() / 2, p.getStaticPaint());

            // Draw the animating circles
            if (p.getAnimatorSet().isRunning()) {
                canvas.drawCircle(p.X, p.Y, p.getCurrentDiameter() / 2, p.getAnimatedCirclePaint());
                needsInvalidate = true;
            }
        }

        if (needsInvalidate) invalidate();
    }

    private void init() {
        setSaveEnabled(true);

        mClickedPoints = new ArrayList<>();
        mLines = new ArrayList<>();

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    final AnimatedPoint newPoint = new AnimatedPoint(motionEvent.getX(), motionEvent.getY());
                    mClickedPoints.add(newPoint);
                    newPoint.setCallback(new AnimatedObjectCallback() {
                        @Override
                        public void animationStarted() {
                            int index = mClickedPoints.indexOf(newPoint);

                            if (index < mClickedPoints.size() - 1) {
                                mLines.get(index).startAnimation();
                            }
                        }

                        @Override
                        public void animationEnded() {
                        }
                    });

                    if (mClickedPoints.size() >= 2) {
                        int numberOfPoints = mClickedPoints.size();

                        final AnimatedLine newLine = new AnimatedLine(mClickedPoints.get(numberOfPoints - 2), mClickedPoints.get(numberOfPoints - 1));
                        mLines.add(newLine);
                        newLine.setCallback(new AnimatedObjectCallback() {
                            @Override
                            public void animationStarted() {
                            }

                            @Override
                            public void animationEnded() {
                                int index = mLines.indexOf(newLine);

                                mClickedPoints.get(index + 1).startAnimation();
                            }
                        });
                    }

                    if (mClickedPoints.size() == mMaxPoints) {
                        mClickedPoints.get(0).startAnimation();
                    }

                    invalidate();
                }

                return true;
            }
        });
    }

    /*
    private Bitmap getScaledBackground() {
        Bitmap unscaled = BitmapFactory.decodeResource(getResources(), R.drawable.world);

        float ratio = (float) unscaled.getWidth() / (float) unscaled.getHeight();

        if (getWidth() > getHeight()) {
            Bitmap scaledX;
            if (ratio < 1) {
                scaledX = Bitmap.createScaledBitmap(unscaled, getWidth(), (int) (getWidth() * ratio), true);
            } else {
                scaledX = Bitmap.createScaledBitmap(unscaled, getWidth(), (int) (getWidth() / ratio), true);
            }

            Bitmap croppedY = scaledX;
            if (scaledX.getHeight() > getHeight()) {
                int yOffset = (scaledX.getHeight() - getHeight()) / 2;
                croppedY = Bitmap.createBitmap(scaledX, 0, yOffset, scaledX.getWidth(), getHeight());
            }

            return croppedY;
        } else {
            Bitmap scaledY;
            if (ratio < 1) {
                scaledY = Bitmap.createScaledBitmap(unscaled, (int) (getHeight() / ratio), getHeight(), false);
            } else {
                scaledY = Bitmap.createScaledBitmap(unscaled, (int) (getHeight() * ratio), getHeight(), false);
            }

            Bitmap croppedX = scaledY;
            if (scaledY.getWidth() > getWidth()) {
                int xOffset = (scaledY.getWidth() - getWidth()) / 2;
                croppedX = Bitmap.createBitmap(scaledY, xOffset, 0, getWidth(), scaledY.getHeight());
            }

            return croppedX;
        }
    }
    */

    private void startAnimatingLines() {
        mClickedPoints.get(0).startAnimation();
    }

    private interface AnimatedObjectCallback {

        void animationStarted();

        void animationEnded();
    }

    public abstract class Point {
        float X, Y;

        public Point() {
            this.X = 0;
            this.Y = 0;
        }

        public Point(float X, float Y) {
            this.X = X;
            this.Y = Y;
        }
    }

    private class AnimatedPoint extends Point {
        AnimatorSet mAnimatorSet;
        ValueAnimator mAnimatorDiameter;
        ValueAnimator mAnimatorAlpha;

        int mDiameterInitial = 20;
        int mDiameterExpanded = 140;
        int mAlphaInitial = 100;
        int mAlphaExpanded = 0;

        Paint mStaticCirclePaint;
        Paint mAnimatedCirclePaint;

        int mCurrentDiameter;

        int mAnimationDuration = 300;

        AnimatedObjectCallback mCallback;

        public AnimatedPoint(float X, float Y) {
            super(X, Y);

            mAnimatorSet = new AnimatorSet();

            initDiameterAnimator();
            initAlphaAnimator();

            mAnimatorSet.setDuration(mAnimationDuration);
            mAnimatorSet.playTogether(mAnimatorAlpha, mAnimatorDiameter);

            mStaticCirclePaint = new Paint();
            mStaticCirclePaint.setColor(Color.YELLOW);

            mAnimatedCirclePaint = new Paint();
            mAnimatedCirclePaint.setColor(Color.WHITE);
        }

        private void initDiameterAnimator() {
            mAnimatorDiameter = ValueAnimator.ofInt(mDiameterInitial, mDiameterExpanded);
            mAnimatorDiameter.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    if (mCurrentDiameter == mDiameterInitial)
                        mCallback.animationStarted();

                    mCurrentDiameter = (int) valueAnimator.getAnimatedValue();

                    if (mCurrentDiameter == mDiameterExpanded)
                        mCallback.animationEnded();
                }
            });
        }

        private void initAlphaAnimator() {
            mAnimatorAlpha = ValueAnimator.ofInt(mAlphaInitial, mAlphaExpanded);
            mAnimatorAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    mAnimatedCirclePaint.setAlpha((int) valueAnimator.getAnimatedValue());
                }
            });
        }

        public void startAnimation() {
            mAnimatorSet.start();
        }

        public int getInitialDiameter() {
            return mDiameterInitial;
        }

        public int getCurrentDiameter() {
            return mCurrentDiameter;
        }

        public Paint getStaticPaint() {
            return mStaticCirclePaint;
        }

        public Paint getAnimatedCirclePaint() {
            return mAnimatedCirclePaint;
        }

        public AnimatorSet getAnimatorSet() {
            return mAnimatorSet;
        }

        public void setCallback(AnimatedObjectCallback callback) {
            this.mCallback = callback;
        }
    }

    private class AnimatedLine {
        AnimatedPoint mOrigin;
        AnimatedPoint mDestination;

        Point mCurrentPoint;

        ValueAnimator mAnimator;
        Paint mPaint;

        int mAnimationDuration = 500;

        AnimatedObjectCallback mCallback;

        public AnimatedLine(AnimatedPoint origin, AnimatedPoint destination) {
            this.mOrigin = origin;
            this.mDestination = destination;

            mCurrentPoint = new AnimatedPoint(mOrigin.X, mOrigin.Y);

            initAnimator();

            mPaint = new Paint();
            mPaint.setStrokeWidth(3f);
            mPaint.setColor(Color.GRAY);
        }

        private void initAnimator() {
            mAnimator = ValueAnimator.ofFloat(mOrigin.X, mDestination.X);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {

                    if (mCurrentPoint.X == mOrigin.X) mCallback.animationStarted();

                    mCurrentPoint.X = (float) valueAnimator.getAnimatedValue();
                    mCurrentPoint.Y = computeYCoordinate(mCurrentPoint.X);

                    if (mCurrentPoint.X == mDestination.X) mCallback.animationEnded();
                }
            });

            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.setDuration(mAnimationDuration);
        }

        public AnimatedPoint getOrigin() {
            return mOrigin;
        }

        public Paint getPaint() {
            return mPaint;
        }

        public Point getCurrentPoint() {
            return mCurrentPoint;
        }

        public ValueAnimator getAnimator() {
            return mAnimator;
        }

        public float computeYCoordinate(float X) {
            return ((mDestination.Y - mOrigin.Y) * (X - mOrigin.X) + mOrigin.Y * mDestination.X - mOrigin.Y * mOrigin.X) / (mDestination.X - mOrigin.X);
        }

        public void startAnimation() {
            mAnimator.start();
        }

        public void setCallback(AnimatedObjectCallback callback) {
            this.mCallback = callback;
        }
    }
}
