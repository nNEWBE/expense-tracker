package com.example.trackexpense.ui.components;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * A custom view that displays a shimmer loading effect.
 * Use this as a placeholder while content is loading.
 */
public class ShimmerView extends View {

    private Paint shimmerPaint;
    private LinearGradient shimmerGradient;
    private Matrix gradientMatrix;
    private float shimmerTranslate = 0f;
    private ValueAnimator shimmerAnimator;

    private int baseColor = 0xFFF0F0F0;
    private int highlightColor = 0xFFFFFFFF;

    private boolean isRounded = true;
    private float cornerRadius = 12f;
    private boolean isCircle = false;

    public ShimmerView(Context context) {
        super(context);
        init(null);
    }

    public ShimmerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ShimmerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientMatrix = new Matrix();
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        if (attrs != null) {
            android.content.res.TypedArray a = getContext().obtainStyledAttributes(attrs,
                    com.example.trackexpense.R.styleable.ShimmerView);
            baseColor = a.getColor(com.example.trackexpense.R.styleable.ShimmerView_shimmer_baseColor, baseColor);
            highlightColor = a.getColor(com.example.trackexpense.R.styleable.ShimmerView_shimmer_highlightColor,
                    highlightColor);
            isCircle = a.getBoolean(com.example.trackexpense.R.styleable.ShimmerView_shimmer_isCircle, isCircle);
            cornerRadius = a.getDimension(com.example.trackexpense.R.styleable.ShimmerView_shimmer_cornerRadius,
                    cornerRadius);
            a.recycle();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) {
            createShimmerGradient();
        }
    }

    private void createShimmerGradient() {
        int width = getWidth();
        if (width <= 0)
            return;

        // Premium gradient: Base -> Highlight -> Base (smoother transition)
        // Using semi-transparent white for highlight to blend better
        shimmerGradient = new LinearGradient(
                0, 0, width, 0,
                new int[] { baseColor, highlightColor, baseColor },
                new float[] { 0.2f, 0.5f, 0.8f },
                Shader.TileMode.CLAMP);
        shimmerPaint.setShader(shimmerGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (shimmerGradient == null)
            return;

        gradientMatrix.setTranslate(shimmerTranslate, 0);
        shimmerGradient.setLocalMatrix(gradientMatrix);

        if (isCircle) {
            float radius = Math.min(getWidth(), getHeight()) / 2f;
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, shimmerPaint);
        } else if (isRounded) {
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(),
                    cornerRadius,
                    cornerRadius,
                    shimmerPaint);
        } else {
            canvas.drawRect(0, 0, getWidth(), getHeight(), shimmerPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startShimmer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopShimmer();
    }

    public void startShimmer() {
        if (shimmerAnimator != null && shimmerAnimator.isRunning()) {
            return;
        }

        // Slower, more elegant animation
        shimmerAnimator = ValueAnimator.ofFloat(-getWidth(), getWidth() * 1.5f);
        shimmerAnimator.setDuration(1800);
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.setRepeatMode(ValueAnimator.RESTART);
        shimmerAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        shimmerAnimator.addUpdateListener(animation -> {
            shimmerTranslate = (float) animation.getAnimatedValue();
            invalidate();
        });
        shimmerAnimator.start();
    }

    public void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
    }

    public void setCircle(boolean circle) {
        this.isCircle = circle;
        invalidate();
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
        invalidate();
    }

    public void setRounded(boolean rounded) {
        this.isRounded = rounded;
        invalidate();
    }

    public void setColors(int baseColor, int highlightColor) {
        this.baseColor = baseColor;
        this.highlightColor = highlightColor;
        createShimmerGradient();
        invalidate();
    }
}
