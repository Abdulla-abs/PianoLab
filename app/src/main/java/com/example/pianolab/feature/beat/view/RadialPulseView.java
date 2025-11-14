package com.example.pianolab.feature.beat.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;


// 超参数（在此此处调整）
// innerRadius: 中心黑圆半径
// ringBaseRadius: 绳圈基础距离（相对于 innerRadius)
// pulseEnergy/pulseDuration: 冲击强度与持续时间（在 pulse() 中设置）
// 预分配的颜色/Shader，避免在 onDraw 中分配对象



public class RadialPulseView extends View {

    // 中心圆和文本
    private Paint centerPaint;
    private Paint textPaint;
    private String centerText = "4"; // 固定显示当前拍数（简化）

    // 绳圈样式
    private Paint ringPaint;
    private Path ringPath = new Path();
    private int ringSegments = 120; // 分段数，越高越平滑
    private float innerRadius; // px
    private float ringBaseRadius; // px, 基础圈半径（相对于 innerRadius）

    // 每段的相位和随机偏移，用于伪造自然震荡
    private float[] phases;
    private float[] offsets;
    private Random rnd = new Random();

    // 全局帧驱动
    private ValueAnimator frameAnimator;
    private long startTime;

    // pulse 控制项（在 pulse() 时设置，然后随时间衰减）
    private volatile float pulseEnergy = 0f; // 叠加能量
    private volatile long pulseStart = 0L;
    private long pulseDuration = 900L; // ms，整体缓冲衰减
    private boolean lastStrong = false;


    private int[] colorsIdle, colorsWeak, colorsStrong;
    private Shader shaderIdle, shaderWeak, shaderStrong;
    // 绳圈上峰值数量（控制环上峰的数量，较小值使峰值更少、更平滑）
    private int bumpCount = 8;

    public RadialPulseView(Context context) {
        this(context, null);
    }

    public RadialPulseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // 超参数初始值（可按需调整）
        innerRadius = dpToPx(48); // 中间黑圆半径
        ringBaseRadius = dpToPx(7); // 更贴近黑实心圆（减小距离）

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(0xFF000000);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dpToPx(18));
        textPaint.setTextAlign(Paint.Align.CENTER);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dpToPx(3f));
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        phases = new float[ringSegments];
        offsets = new float[ringSegments];
        for (int i = 0; i < ringSegments; i++) {
            phases[i] = rnd.nextFloat() * (float) (Math.PI * 2);
            // 降低初始随机偏移，减少震荡量
            offsets[i] = (rnd.nextFloat() - 0.5f) * dpToPx(0.8f);
        }

        frameAnimator = ValueAnimator.ofFloat(0f, 1f);
        frameAnimator.setDuration(100000);
        frameAnimator.setRepeatCount(ValueAnimator.INFINITE);
        frameAnimator.addUpdateListener(animation -> {
            invalidate();
            // lazy startTime
            if (startTime == 0L) startTime = SystemClock.uptimeMillis();
        });
        // 立即启动帧动画（轻量级，只做 invalidate）
        frameAnimator.start();
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cx = w / 2f;
        float cy = h / 2f;
        // 初始化三套颜色与 Shader，避免在 onDraw 中频繁创建
        // 强拍颜色：红 - 橙 - 黄，呈现剧烈对比
        colorsStrong = new int[]{0xFFB71C1C, 0xFFFF6F00, 0xFFFFEB3B, 0xFFB71C1C};
        // 弱拍颜色：深蓝 -> 青绿 -> 绿色，较深的蓝色系但带有对比
        colorsWeak = new int[]{0xFF0D47A1, 0xFF00796B, 0xFF2E7D32, 0xFF0D47A1};
        // 静止时的灰色色带
        colorsIdle = new int[]{0xFFDDDDDD, 0xFFBBBBBB, 0xFF888888, 0xFFDDDDDD};
        shaderStrong = new SweepGradient(cx, cy, colorsStrong, null);
        shaderWeak = new SweepGradient(cx, cy, colorsWeak, null);
        shaderIdle = new SweepGradient(cx, cy, colorsIdle, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        // 中心圆与文字
        canvas.drawCircle(cx, cy, innerRadius, centerPaint);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(centerText, cx, textY, textPaint);

        // 计算时间与 pulse 状态
        long now = SystemClock.uptimeMillis();
        float t = (startTime == 0L) ? 0f : (now - startTime) / 1000f; // 秒

        float pulseFactor = 0f;
        boolean active = false;
        if (pulseStart > 0) {
            float dt = (now - pulseStart) / (float) pulseDuration;
            if (dt < 1f) {
                active = true;
                pulseFactor = pulseEnergy * (float) Math.pow(1f - dt, 2);
            } else {
                pulseStart = 0L;
                pulseEnergy = 0f;
            }
        }

        // 根据状态选择预生成的渐变 Shader，并设置合适的线宽（避免在 onDraw 中分配对象）
        if (active) {
            ringPaint.setShader(lastStrong ? shaderStrong : shaderWeak);
            ringPaint.setStrokeWidth(lastStrong ? dpToPx(3.6f) : dpToPx(2.6f));
        } else {
            ringPaint.setShader(shaderIdle);
            ringPaint.setStrokeWidth(dpToPx(2.2f));
        }

        ringPath.reset();
        float twoPi = (float) (Math.PI * 2);

        if (!active) {
            // 停止时画标准圆形（无噪声），更贴近内圆
            float radius = innerRadius + ringBaseRadius;
            ringPath.addCircle(cx, cy, radius, Path.Direction.CW);
            canvas.drawPath(ringPath, ringPaint);
            return;
        }

        // 活跃时绘制带平滑、较小幅度的扰动环
        float prevX = 0f, prevY = 0f;
        for (int i = 0; i < ringSegments; i++) {
            float ang = twoPi * i / ringSegments - (float) Math.PI / 2f; // 从顶部开始

            // 平滑、减小振幅的噪声
            // 降低高频噪声，增强低频“摆动”效果，使峰值数量可控且过渡更自然
            float slowWave = (float) Math.sin(phases[i] + t * 0.45f) * dpToPx(2.2f);
            float fastWave = (float) Math.sin(phases[i] * 1.1f + t * 2.2f) * dpToPx(0.3f);
            float baseNoise = offsets[i] + slowWave * 0.18f + fastWave * 0.06f;

            // 使用 bumpCount 控制圈上峰值数量，降低空间频率使峰值更少、更平滑
            float localBump = 0.6f + 0.4f * (float) Math.sin(twoPi * bumpCount * i / ringSegments + phases[i] * 0.35f);

            float radius = innerRadius + ringBaseRadius + baseNoise + pulseFactor * dpToPx(12f) * localBump;

            float x = cx + (float) Math.cos(ang) * radius;
            float y = cy + (float) Math.sin(ang) * radius;

            if (i == 0) {
                ringPath.moveTo(x, y);
            } else {
                float cxm = (prevX + x) / 2f;
                float cym = (prevY + y) / 2f;
                ringPath.quadTo(prevX, prevY, cxm, cym);
            }
            prevX = x;
            prevY = y;
        }

        // 封闭曲线，连接最后到起点
        float ang0 = - (float) Math.PI / 2f;
        float radius0 = innerRadius + ringBaseRadius + offsets[0] + pulseFactor * dpToPx(12f) * (0.6f + 0.4f * (float) Math.sin(phases[0] * 0.7f + 0));
        float x0 = cx + (float) Math.cos(ang0) * radius0;
        float y0 = cy + (float) Math.sin(ang0) * radius0;
        ringPath.quadTo(prevX, prevY, (prevX + x0) / 2f, (prevY + y0) / 2f);
        ringPath.close();

        canvas.drawPath(ringPath, ringPaint);
    }

    /**
     * Trigger a pulse animation. Call on each beat. strong==true for downbeat
     */
    public void pulse(boolean strong) {
        lastStrong = strong;
        // 设置 pulse 能量，强拍更强
        pulseEnergy = strong ? 1.25f : 0.6f;
        pulseStart = SystemClock.uptimeMillis();
        pulseDuration = strong ? 900L : 600L;

        // 给相位带一点抖动，但幅度更小以保持自然
        for (int i = 0; i < ringSegments; i++) {
            phases[i] += (rnd.nextFloat() - 0.5f) * 0.45f;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (frameAnimator != null) {
            frameAnimator.cancel();
            frameAnimator = null;
        }
    }
}
