package com.example.adimarket;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom Bar Chart View yang mendukung gradien, animasi, dan label.
 */
public class BarChartView extends View {

    private Paint barPaint, axisPaint, labelPaint, valuePaint, gridPaint;
    private float[] data;
    private String[] labels;
    private int[] colors;
    private float animProgress = 1f;
    private String title = "";

    public BarChartView(Context context) { super(context); init(); }
    public BarChartView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public BarChartView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(0xFFCCCCCC);
        axisPaint.setStrokeWidth(2f);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFEEEEEE);
        gridPaint.setStrokeWidth(1f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF888888);
        labelPaint.setTextSize(28f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(0xFF333333);
        valuePaint.setTextSize(26f);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void setData(float[] data, String[] labels, int[] colors, String title) {
        this.data = data;
        this.labels = labels;
        this.colors = colors;
        this.title = title;
        invalidate();
    }

    public void setAnimProgress(float p) {
        this.animProgress = p;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.length == 0) return;

        int w = getWidth();
        int h = getHeight();
        float padL = 60f, padR = 20f, padT = 40f, padB = 60f;
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;

        float max = 0;
        for (float d : data) if (d > max) max = d;
        if (max == 0) max = 1;

        // Grid lines
        int gridCount = 5;
        for (int i = 0; i <= gridCount; i++) {
            float y = padT + chartH - (chartH * i / gridCount);
            canvas.drawLine(padL, y, padL + chartW, y, gridPaint);
            String gridLabel = String.valueOf((int)(max * i / gridCount));
            canvas.drawText(gridLabel, padL - 8f, y + 8f, labelPaint);
        }

        // Axis lines
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint);
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint);

        // Bars
        float barGroup = chartW / data.length;
        float barW = barGroup * 0.6f;
        float gap = barGroup * 0.2f;

        for (int i = 0; i < data.length; i++) {
            float barH = (data[i] / max) * chartH * animProgress;
            float left = padL + gap + i * barGroup;
            float right = left + barW;
            float top = padT + chartH - barH;
            float bottom = padT + chartH;

            // Gradient color
            int baseColor = (colors != null && i < colors.length) ? colors[i] : 0xFF4CAF50;
            int lightColor = lighten(baseColor, 0.4f);
            LinearGradient grad = new LinearGradient(left, top, right, bottom,
                    lightColor, baseColor, Shader.TileMode.CLAMP);
            barPaint.setShader(grad);

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, 12f, 12f, barPaint);

            // Value label on top of bar
            if (animProgress >= 0.95f) {
                String val = (data[i] == (int) data[i])
                        ? String.valueOf((int) data[i])
                        : String.format("%.1f", data[i]);
                canvas.drawText(val, left + barW / 2f, top - 8f, valuePaint);
            }

            // X label
            if (labels != null && i < labels.length) {
                canvas.drawText(labels[i], left + barW / 2f, padT + chartH + 44f, labelPaint);
            }
        }
    }

    private int lighten(int color, float factor) {
        int r = Math.min(255, (int)(Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, (int)(Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, (int)(Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.rgb(r, g, b);
    }
}
