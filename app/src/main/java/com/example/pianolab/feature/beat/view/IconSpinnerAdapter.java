
package com.example.pianolab.feature.beat.view;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pianolab.R;

import java.util.List;

public class IconSpinnerAdapter extends ArrayAdapter<Integer> {
    private final LayoutInflater inflater;

    public IconSpinnerAdapter(@NonNull Context context, @NonNull List<Integer> icons) {
        super(context, 0, icons);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView != null ? convertView :
                inflater.inflate(R.layout.spinner_item_image, parent, false);
        ImageView iv = view.findViewById(R.id.iv_icon);
        Integer resId = getItem(position);
        if (resId != null) iv.setImageResource(resId);
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView != null ? convertView :
                inflater.inflate(R.layout.spinner_dropdown_item_image, parent, false);
        ImageView iv = view.findViewById(R.id.iv_dropdown_icon);
        Integer resId = getItem(position);
        if (resId != null) iv.setImageResource(resId);
        return view;
    }
}
