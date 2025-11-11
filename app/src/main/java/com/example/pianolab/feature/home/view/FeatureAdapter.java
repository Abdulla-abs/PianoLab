package com.example.pianolab.feature.home.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.pianolab.R;
import com.example.pianolab.feature.home.model.FeatureItem;
import java.util.List;


public class FeatureAdapter extends ArrayAdapter<FeatureItem> {
    private final LayoutInflater inflater;
    private final int resourceId;

    public FeatureAdapter(Context context, int resource, List<FeatureItem> items) {
        super(context, resource, items);
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {

            convertView = inflater.inflate(resourceId, parent, false);

            holder = new ViewHolder();
            holder.ivIcon = convertView.findViewById(R.id.iv_feature_icon);
            holder.tvName = convertView.findViewById(R.id.tv_feature_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        FeatureItem item = getItem(position);
        if (item != null) {
            holder.ivIcon.setImageResource(item.getIconResId());
            holder.tvName.setText(item.getName());
        }
        return convertView;
    }


    static class ViewHolder {
        ImageView ivIcon;
        TextView tvName;
    }
}