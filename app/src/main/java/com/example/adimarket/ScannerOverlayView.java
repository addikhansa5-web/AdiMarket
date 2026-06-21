package com.example.adimarket;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ScannerOverlayView extends View {
    private Paint paintMask;
    private Paint paintBorder;
    private Paint paintLaser;
    private RectF boxRect;
    private float laserY = 0f;
    private boolean movingDown = true;
    private float cornerRadius = 24f;

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintMask = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMask.setColor(Color.parseColor("#AA000000")); // 66% opacity black

        paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorder.setColor(Color.parseColor("#00E676")); // Neon green border
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(6f);

        paintLaser = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLaser.setColor(Color.parseColor("#00E676"));
        paintLaser.setStyle(Paint.Style.FILL);
        paintLaser.setStrokeWidth(5f);

        boxRect = new RectF();
        // Disable hardware acceleration to allow PorterDuff.Mode.CLEAR masking
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Center box: width is 85% of screen width, height fits standard card ratio (1.58:1)
        float boxWidth = w * 0.85f;
        float boxHeight = boxWidth / 1.58f;
        float left = (w - boxWidth) / 2f;
        float top = (h - boxHeight) / 2f - 40f; // Offset upwards slightly
        boxRect.set(left, top, left + boxWidth, top + boxHeight);
        laserY = top;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw solid dark overlay
        canvas.drawRect(0, 0, getWidth(), getHeight(), paintMask);

        // 2. Clear the center box path (cutout)
        Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, clearPaint);

        // 3. Draw neon border around cutout
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, paintBorder);

        // 4. Draw animating laser scanner line
        if (laserY < boxRect.top) laserY = boxRect.top;
        if (laserY > boxRect.bottom) laserY = boxRect.bottom;

        canvas.drawLine(boxRect.left + 8, laserY, boxRect.right - 8, laserY, paintLaser);

        // Update laser position
        float speed = 6f;
        if (movingDown) {
            laserY += speed;
            if (laserY >= boxRect.bottom - 8) {
                movingDown = false;
            }
        } else {
            laserY -= speed;
            if (laserY <= boxRect.top + 8) {
                movingDown = true;
            }
        }

        // Force redraw to animate smoothly (approx 60fps)
        postInvalidateDelayed(16);
    }

    public RectF getBoxRect() {
        return boxRect;
    }
}
