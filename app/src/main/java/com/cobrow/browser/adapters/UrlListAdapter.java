package com.cobrow.browser.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cobrow.browser.R;

import java.util.List;

public class UrlListAdapter extends RecyclerView.Adapter<UrlListAdapter.VH> {
    public interface OnItemClick { void onClick(int pos); }

    private final List<String> items;
    private final OnItemClick onClick;
    private final OnItemClick onDelete;

    public UrlListAdapter(List<String> items, OnItemClick onClick, OnItemClick onDelete) {
        this.items = items;
        this.onClick = onClick;
        this.onDelete = onDelete;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_url, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String[] parts = items.get(pos).split("\n", 2);
        h.title.setText(parts[0]);
        h.url.setText(parts.length > 1 ? parts[1] : "");
        h.itemView.setOnClickListener(v -> onClick.onClick(h.getAdapterPosition()));
        if (onDelete != null) {
            h.btnDelete.setVisibility(View.VISIBLE);
            h.btnDelete.setOnClickListener(v -> onDelete.onClick(h.getAdapterPosition()));
        } else {
            h.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, url;
        ImageButton btnDelete;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.itemTitle);
            url = v.findViewById(R.id.itemUrl);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
