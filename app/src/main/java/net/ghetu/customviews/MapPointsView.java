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
 * <p>
 * Animates a pathway between a number of points in the view. The points can be chosen by the user in 2 ways: <br>
 * - actively touching the screen (in which case the animation starts when a minimum number of points is achieved) <br>
 * - manually by providing the X and Y offsets (from the top-left corner) for the MDPI image version <br>
 * <p/>
 * <p>
 * The view animates 2 things: the pathway from one point to another and a point around each circle once the
 * pathway reaches is. Animation speeds are customizable, as well as colors for the points and pathways. Because the
 * view extends from {@link ImageView}, the background can be set regularly, as well as the scale type.
 * </p>
 */
public class MapPointsView extends ImageView {
    private static final String TAG = "MapPointsView";

    /**
     * Points between which the pathways are formed
     */
    ArrayList<AnimatedPoint> mPoints;

    /**
     * Pathways
     */
    ArrayList<AnimatedLine> mLines;

    boolean mSelectPointsByTouching;
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
    protected void onDraw(Canvas canvas) {
        // Ensure that the view behaves like an ImageView
        super.onDraw(canvas);

        boolean needsInvalidate = false;

        for (AnimatedLine l : mLines) {
            // If the pathway is currently being animated, getCurrentPoint is different from the destination as well
            // as the origin
            canvas.drawLine(l.getOrigin().X, l.getOrigin().Y, l.getCurrentPoint().X, l.getCurrentPoint().Y, l
                    .getPaint());

            if (l.getAnimator().isRunning())
                needsInvalidate = true;
        }

        for (AnimatedPoint p : mPoints) {
            // Draw the fixed points
            canvas.drawCircle(p.X, p.Y, p.getInitialDiameter() / 2, p.getStaticPaint());

            // Draw the animating circles that surround the points
            if (p.getAnimatorSet().isRunning()) {
                canvas.drawCircle(p.X, p.Y, p.getCurrentDiameter() / 2, p.getAnimatedCirclePaint());
                needsInvalidate = true;
            }
        }

        // If an ongoing animation was detected, the view should probably drawn again to make sure that no frames are
        // skipped
        if (needsInvalidate) invalidate();
    }

    /**
     * Sets up the members, as well as callbacks for the animations (to determine the precedence of animation between
     * the pathways and points).
     */
    private void init() {
        // TODO: Implement state staving when view gets destroyed and created again
        setSaveEnabled(true);

        mPoints = new ArrayList<>();
        mLines = new ArrayList<>();

        if (mSelectPointsByTouching) {
            this.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {

                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                        final AnimatedPoint newPoint = new AnimatedPoint(motionEvent.getX(), motionEvent.getY());
                        mPoints.add(newPoint);
                        newPoint.setCallback(getPointCallback(newPoint));

                        if (mPoints.size() >= 2) {
                            int numberOfPoints = mPoints.size();

                            // Add a new pathway between the last 2 points
                            final AnimatedLine newLine = new AnimatedLine(mPoints.get(numberOfPoints - 2),
                                    mPoints.get(numberOfPoints - 1));
                            mLines.add(newLine);
                            newLine.setCallback(getLineCallback(newLine));
                        }

                        if (mPoints.size() == mMaxPoints) {
                            mPoints.get(0).startAnimation();
                        }

                        // New points were added, an animation might have been started. Time to redraw the view to
                        // reflect these changes!
                        invalidate();

                        return true;
                    }

                    return false;
                }
            });
        } else {
            // If the points have already been selected in a static manner, just describe how the path should be
            // animated by setting the proper callbacks
            for (AnimatedPoint p : mPoints) {
                p.setCallback(getPointCallback(p));
            }

            for (int i = 0; i < mPoints.size() - 1; i++) {
                // Add a new pathway between the last 2 points
                final AnimatedLine newLine = new AnimatedLine(mPoints.get(i),
                        mPoints.get(i + 1));
                mLines.add(newLine);
                newLine.setCallback(getLineCallback(newLine));
            }
        }

    }

    /**
     * Returns a {@link net.ghetu.customviews.MapPointsView.AnimatedObjectCallback} so that once the point begins its
     * animation, the next line on the path begins its animation as well.
     *
     * @param newPoint
     * @return
     */
    private AnimatedObjectCallback getPointCallback(final AnimatedPoint newPoint) {
        return new AnimatedObjectCallback() {
            @Override
            public void animationStarted() {
                // When the outer circle of the point starts its animation, start animating the outgoing
                // path as well (if there is one)
                int index = mPoints.indexOf(newPoint);

                if (index < mPoints.size() - 1) {
                    mLines.get(index).startAnimation();
                }
            }

            @Override
            public void animationEnded() {

            }
        };
    }

    /**
     * Returns a {@link net.ghetu.customviews.MapPointsView.AnimatedObjectCallback} so that once the line finishes
     * its animation, the next point on the path begins its animation.
     *
     * @param newLine
     * @return
     */
    private AnimatedObjectCallback getLineCallback(final AnimatedLine newLine) {
        return new AnimatedObjectCallback() {
            @Override
            public void animationStarted() {

            }

            @Override
            public void animationEnded() {
                // When the animation of the pathway has ended, start animating the next point
                int index = mLines.indexOf(newLine);

                mPoints.get(index + 1).startAnimation();
            }
        };
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

    /**
     * Start animating the pathway. Should be used when mSelectPointsByTouching is false to start up the animation.
     */
    private void startAnimatingLines() {
        mPoints.get(0).startAnimation();
    }

    /**
     * Provides callbacks for the start and end of an animation.
     */
    private interface AnimatedObjectCallback {

        /**
         * Called when the animation has started.
         */
        void animationStarted();

        /**
         * Called when the animation has ended.
         */
        void animationEnded();
    }

    // Simple representation of a point
    public class Point {
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

    /**
     * A point on the pathway. The animation consists of a circle around the point that expands and becomes gradually
     * transparent.
     */
    private class AnimatedPoint extends Point {
        // Alpha and diameter are animated in parallel, so an {@link AnimatorSet} is necessary
        AnimatorSet mAnimatorSet;
        ValueAnimator mAnimatorDiameter;
        ValueAnimator mAnimatorAlpha;

        int mDiameterInitial = 20;
        int mDiameterExpanded = 140;
        int mAlphaInitial = 100;
        int mAlphaExpanded = 0;

        Paint mStaticCirclePaint;

        // The alpha value of this member is animated
        Paint mAnimatedCirclePaint;

        // This member is animated
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

        /**
         * Sets up the way in which the circle that surrounds the point expands
         */
        private void initDiameterAnimator() {
            // Animate between these two values
            mAnimatorDiameter = ValueAnimator.ofInt(mDiameterInitial, mDiameterExpanded);
            mAnimatorDiameter.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    if (mCurrentDiameter == mDiameterInitial)
                        mCallback.animationStarted();

                    // Animate the outer circle's diameter
                    mCurrentDiameter = (int) valueAnimator.getAnimatedValue();

                    if (mCurrentDiameter == mDiameterExpanded)
                        mCallback.animationEnded();
                }
            });
        }

        private void initAlphaAnimator() {
            // Animate between these two values
            mAnimatorAlpha = ValueAnimator.ofInt(mAlphaInitial, mAlphaExpanded);
            mAnimatorAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    // Animate the Paint object's alpha value
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

    /**
     * A line on the pathway. The animation consists of a line that starts off from just one pixel and gradually
     * grows until it links 2 {@link net.ghetu.customviews.MapPointsView.AnimatedPoint}s.
     */
    private class AnimatedLine {
        /**
         * Origin of the line
         */
        AnimatedPoint mOrigin;

        /**
         * Destination of the line
         */
        AnimatedPoint mDestination;

        /**
         * Current point in the animation. This point coincides with the origin at the beginning of the animation. By
         * the end of the animation, it coincides with the destination. If the animation has started, while the view
         * gets drawn again and again, the line will appear to grow from just one point in the beginning to a full
         * line from origin to destination in the end
         */
        Point mCurrentPoint;

        ValueAnimator mAnimator;
        Paint mPaint;

        int mAnimationDuration = 500;

        AnimatedObjectCallback mCallback;

        public AnimatedLine(AnimatedPoint origin, AnimatedPoint destination) {
            this.mOrigin = origin;
            this.mDestination = destination;

            mCurrentPoint = new Point(mOrigin.X, mOrigin.Y);

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
                    // If the X value of mCurrentPoint equals the one of mOrigin, then the animation has just begun
                    if (mCurrentPoint.X == mOrigin.X) mCallback.animationStarted();

                    mCurrentPoint.X = (float) valueAnimator.getAnimatedValue();
                    mCurrentPoint.Y = computeYCoordinate(mCurrentPoint.X);

                    // If the X value of mCurrentPoint equals the one of mDestination, then the animation finished
                    if (mCurrentPoint.X == mDestination.X) mCallback.animationEnded();
                }
            });

            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.setDuration(mAnimationDuration);
        }

        /**
         * While the X value of the animated extremity of line varies between the origin's and the destination's, the
         * Y value can be calculated using the two-point form of the equation of a straight line. This function
         * returns the Y value for a given X value.
         *
         * @param X
         * @return
         */
        public float computeYCoordinate(float X) {
            // Line equation (x - xOrigin) / (xDestination - xOrigin) = (y - yOrigin) / (yDestination - yOrigin)
            return ((mDestination.Y - mOrigin.Y) * (X - mOrigin.X) + mOrigin.Y * mDestination.X - mOrigin.Y * mOrigin
                    .X) / (mDestination.X - mOrigin.X);
        }

        public void startAnimation() {
            mAnimator.start();
        }

        public void setCallback(AnimatedObjectCallback callback) {
            this.mCallback = callback;
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
    }
}
