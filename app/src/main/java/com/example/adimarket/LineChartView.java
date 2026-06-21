package com.example.adimarket;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom Line Chart View dengan area fill dan animasi.
 */
public class LineChartView extends View {

    private Paint linePaint, fillPaint, dotPaint, labelPaint, valuePaint, gridPaint, axisPaint;
    private float[] data;
    private String[] labels;
    private int lineColor = 0xFF1976D2;
    private float animProgress = 1f;

    public LineChartView(Context context) { super(context); init(); }
    public LineChartView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public LineChartView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(0xFFCCCCCC);
        axisPaint.setStrokeWidth(2f);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFEEEEEE);
        gridPaint.setStrokeWidth(1f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF888888);
        labelPaint.setTextSize(26f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        valuePaint.setTextSize(24f);
    }

    public void setData(float[] data, String[] labels, int lineColor) {
        this.data = data;
        this.labels = labels;
        this.lineColor = lineColor;
        linePaint.setColor(lineColor);
        dotPaint.setColor(lineColor);
        valuePaint.setColor(lineColor);
        invalidate();
    }

    public void setAnimProgress(float p) {
        this.animProgress = p;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.length < 2) return;

        int w = getWidth(), h = getHeight();
        float padL = 60f, padR = 20f, padT = 40f, padB = 60f;
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;

        float max = 0;
        for (float d : data) if (d > max) max = d;
        if (max == 0) max = 1;

        // Grid
        for (int i = 0; i <= 5; i++) {
            float y = padT + chartH - (chartH * i / 5f);
            canvas.drawLine(padL, y, padL + chartW, y, gridPaint);
            canvas.drawText(String.valueOf((int)(max * i / 5)), padL - 8f, y + 8f, labelPaint);
        }
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint);
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint);

        int drawCount = Math.max(2, (int)(data.length * animProgress));

        // Calculate points
        float[] px = new float[data.length];
        float[] py = new float[data.length];
        float step = chartW / (data.length - 1);
        for (int i = 0; i < data.length; i++) {
            px[i] = padL + i * step;
            py[i] = padT + chartH - (data[i] / max) * chartH;
        }

        // Fill area
        Path fillPath = new Path();
        fillPath.moveTo(px[0], padT + chartH);
        for (int i = 0; i < drawCount; i++) {
            if (i == 0) fillPath.lineTo(px[0], py[0]);
            else fillPath.lineTo(px[i], py[i]);
        }
        fillPath.lineTo(px[drawCount - 1], padT + chartH);
        fillPath.close();

        LinearGradient fillGrad = new LinearGradient(0, padT, 0, padT + chartH,
                Color.argb(80, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP);
        fillPaint.setShader(fillGrad);
        canvas.drawPath(fillPath, fillPaint);

        // Line
        Path linePath = new Path();
        for (int i = 0; i < drawCount; i++) {
            if (i == 0) linePath.moveTo(px[0], py[0]);
            else linePath.lineTo(px[i], py[i]);
        }
        canvas.drawPath(linePath, linePaint);

        // Dots & labels
        for (int i = 0; i < drawCount; i++) {
            // Outer white circle
            Paint whiteDot = new Paint(Paint.ANTI_ALIAS_FLAG);
            whiteDot.setColor(Color.WHITE);
            canvas.drawCircle(px[i], py[i], 12f, whiteDot);
            canvas.drawCircle(px[i], py[i], 8f, dotPaint);

            if (i == drawCount - 1 || animProgress >= 0.95f) {
                String val = (data[i] == (int)data[i])
                        ? String.valueOf((int)data[i])
                        : String.format("%.1f", data[i]);
                canvas.drawText(val, px[i], py[i] - 18f, valuePaint);
            }
            if (labels != null && i < labels.length) {
                canvas.drawText(labels[i], px[i], padT + chartH + 44f, labelPaint);
            }
        }
    }
}
