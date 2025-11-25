package com.example.pianolab.feature.virtual_piano.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * 一个不响应滚动手势的 HorizontalScrollView，
 * 不拦截触摸事件，也不处理触摸事件，允许子视图接收触摸（用于点击琴键）
 */
public class NonScrollableHorizontalScrollView extends HorizontalScrollView {

    public NonScrollableHorizontalScrollView(Context context) {
        super(context);
    }

    public NonScrollableHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonScrollableHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 不拦截，交给子视图（PianoView）处理点击事件
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 不处理触摸事件（禁止滑动）
        return false;
    }
}