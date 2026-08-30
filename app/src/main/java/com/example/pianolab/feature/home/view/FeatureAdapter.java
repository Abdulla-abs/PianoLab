package com.example.pianolab.feature.home.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pianolab.databinding.ItemFeatureCardBinding;
import com.example.pianolab.feature.home.model.FeatureItem;
import java.util.List;

public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder> {

  private final List<FeatureItem> items;
  private final OnItemClickListener listener;

  public interface OnItemClickListener {
    void onItemClick(FeatureItem item);
  }

  public FeatureAdapter(List<FeatureItem> items, OnItemClickListener listener) {
    this.items = items;
    this.listener = listener;
  }

  @NonNull
  @Override
  public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    ItemFeatureCardBinding binding = ItemFeatureCardBinding.inflate(layoutInflater, parent, false);
    return new FeatureViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
    FeatureItem item = items.get(position);
    holder.bind(item, listener);
  }

  @Override
  public int getItemCount() {
    return items != null ? items.size() : 0;
  }

  public static class FeatureViewHolder extends RecyclerView.ViewHolder {
    private final ItemFeatureCardBinding binding;

    public FeatureViewHolder(ItemFeatureCardBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    public void bind(FeatureItem item, OnItemClickListener listener) {
      binding.setItem(item);
      binding.ivFeatureIcon.setImageResource(item.getIconResId());
      binding.ivFeatureIcon.setContentDescription(
          itemView.getContext().getString(item.getNameResId()));
      binding.ivFeatureIcon.setColorFilter(
          ContextCompat.getColor(itemView.getContext(), item.getIconTintColorResId()));
      binding.tvFeatureName.setText(itemView.getContext().getString(item.getNameResId()));
      binding.tvFeatureDescription.setText(
          itemView.getContext().getString(item.getDescriptionResId()));
      binding.featureCardRoot.setBackgroundResource(item.getCardBackgroundResId());
      binding.iconCircle.setBackgroundResource(item.getIconCircleBackgroundResId());
      binding.executePendingBindings();
      itemView.setOnClickListener(v -> listener.onItemClick(item));
    }
  }
}
