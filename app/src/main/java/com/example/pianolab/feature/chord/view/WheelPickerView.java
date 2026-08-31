package com.example.pianolab.feature.chord.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianolab.R;

public class WheelPickerView extends FrameLayout {

    public interface OnValueChangeListener {
        void onValueChanged(WheelPickerView picker, int oldValue, int newValue);
    }

    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;
    private final LinearSnapHelper snapHelper;
    private final int itemHeightPx;
    private final int selectedColor;
    private final int unselectedColor;

    private WheelAdapter adapter;
    private String[] displayedValues = new String[0];
    private int selectedIndex;
    private int highlightedIndex;
    private OnValueChangeListener onValueChangeListener;
    private boolean userScroll;

    public WheelPickerView(@NonNull Context context) {
        this(context, null);
    }

    public WheelPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        itemHeightPx = getResources().getDimensionPixelSize(R.dimen.chord_picker_wheel_item_height);
        selectedColor = ContextCompat.getColor(context, R.color.chord_picker_wheel_selected);
        unselectedColor = ContextCompat.getColor(context, R.color.chord_picker_wheel_unselected);

        recyclerView = new RecyclerView(context);
        layoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(true);

        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateHighlightedItem(false);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    userScroll = true;
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    syncSelectionFromSnap();
                }
            }
        });

        recyclerView.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                ViewParent parent = v.getParent();
                while (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                    if (parent instanceof NestedScrollView) {
                        break;
                    }
                    parent = parent.getParent();
                }
            }
            return false;
        });

        addView(recyclerView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setDisplayedValues(@NonNull String[] values) {
        displayedValues = values;
        selectedIndex = Math.min(selectedIndex, Math.max(values.length - 1, 0));
        highlightedIndex = selectedIndex;
        adapter = new WheelAdapter(values);
        recyclerView.setAdapter(adapter);
        post(this::applyCenterPaddingAndScroll);
    }

    public int getValue() {
        return selectedIndex;
    }

    public void setValue(int index) {
        if (displayedValues.length == 0) {
            selectedIndex = 0;
            highlightedIndex = 0;
            return;
        }
        int clamped = Math.max(0, Math.min(index, displayedValues.length - 1));
        int oldValue = selectedIndex;
        selectedIndex = clamped;
        highlightedIndex = clamped;
        applyCenterPaddingAndScroll();
        if (oldValue != clamped && onValueChangeListener != null) {
            onValueChangeListener.onValueChanged(this, oldValue, clamped);
        }
    }

    public void setOnValueChangedListener(@Nullable OnValueChangeListener listener) {
        onValueChangeListener = listener;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        applyCenterPaddingAndScroll();
    }

    private void applyCenterPaddingAndScroll() {
        if (adapter == null || recyclerView.getHeight() <= 0) {
            return;
        }
        int verticalPadding = Math.max(0, (recyclerView.getHeight() - itemHeightPx) / 2);
        recyclerView.setPadding(0, verticalPadding, 0, verticalPadding);
        recyclerView.setClipToPadding(false);
        layoutManager.scrollToPositionWithOffset(selectedIndex, 0);
        highlightedIndex = selectedIndex;
        refreshVisibleItemStyles();
    }

    private void updateHighlightedItem(boolean force) {
        if (adapter == null || layoutManager.getChildCount() == 0) {
            return;
        }
        int centerY = recyclerView.getHeight() / 2;
        int closestPos = highlightedIndex;
        int closestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            View child = layoutManager.getChildAt(i);
            if (child == null) {
                continue;
            }
            int childCenter = (child.getTop() + child.getBottom()) / 2;
            int distance = Math.abs(childCenter - centerY);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPos = layoutManager.getPosition(child);
            }
        }
        if (closestPos < 0 || closestPos >= displayedValues.length) {
            return;
        }
        if (closestPos != highlightedIndex) {
            highlightedIndex = closestPos;
            refreshVisibleItemStyles();
        }
        if (force) {
            selectedIndex = closestPos;
        }
    }

    private void refreshVisibleItemStyles() {
        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            View child = layoutManager.getChildAt(i);
            int position = layoutManager.getPosition(child);
            if (position == RecyclerView.NO_POSITION) {
                continue;
            }
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            if (holder instanceof WheelViewHolder) {
                ((WheelViewHolder) holder).bind(
                        displayedValues[position], position == highlightedIndex);
            }
        }
    }

    private void syncSelectionFromSnap() {
        View snapView = snapHelper.findSnapView(layoutManager);
        if (snapView == null) {
            return;
        }
        int newIndex = layoutManager.getPosition(snapView);
        if (newIndex < 0 || newIndex >= displayedValues.length) {
            return;
        }
        int oldIndex = selectedIndex;
        selectedIndex = newIndex;
        highlightedIndex = newIndex;
        refreshVisibleItemStyles();
        if (userScroll && oldIndex != newIndex && onValueChangeListener != null) {
            onValueChangeListener.onValueChanged(this, oldIndex, newIndex);
        }
        userScroll = false;
    }

    private final class WheelAdapter extends RecyclerView.Adapter<WheelViewHolder> {

        private final String[] items;

        private WheelAdapter(String[] items) {
            this.items = items;
        }

        @NonNull
        @Override
        public WheelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wheel_picker, parent, false);
            return new WheelViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WheelViewHolder holder, int position) {
            holder.bind(items[position], position == highlightedIndex);
        }

        @Override
        public int getItemCount() {
            return items.length;
        }
    }

    private final class WheelViewHolder extends RecyclerView.ViewHolder {

        private final TextView label;

        private WheelViewHolder(@NonNull View itemView) {
            super(itemView);
            label = (TextView) itemView;
        }

        private void bind(String text, boolean selected) {
            label.setText(text);
            label.setTextColor(selected ? selectedColor : unselectedColor);
            label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }
}
