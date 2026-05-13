package com.cobrow.browser.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cobrow.browser.R;
import com.cobrow.browser.data.Tab;

import java.util.ArrayList;
import java.util.List;

public class TabsGridAdapter extends RecyclerView.Adapter<TabsGridAdapter.VH> {

    public interface Listener {
        void onTabClicked(int position);
        void onTabClosed(int position);
        void onTabCloned(int position);
    }

    private List<Tab> items = new ArrayList<>();
    private final Listener listener;

    public TabsGridAdapter(Listener listener) { this.listener = listener; }

    public void setItems(List<Tab> tabs) { items = tabs != null ? tabs : new ArrayList<>(); notifyDataSetChanged(); }

    public List<Tab> getItems() { return items; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Tab t = items.get(position);
        holder.tvTitle.setText(t.title != null && !t.title.isEmpty() ? t.title : t.url);
        holder.tvUrl.setText(t.url != null ? t.url : "");
        if (t.thumbnail != null && !t.thumbnail.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(t.thumbnail, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.ivThumb.setImageBitmap(bmp);
            } catch (Exception e) { holder.ivThumb.setImageResource(R.drawable.ic_launcher_foreground); }
        } else {
            holder.ivThumb.setImageResource(R.drawable.ic_launcher_foreground);
        }

        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onTabClicked(holder.getAdapterPosition()); });
        holder.btnClose.setOnClickListener(v -> { if (listener != null) listener.onTabClosed(holder.getAdapterPosition()); });
        holder.btnClone.setOnClickListener(v -> { if (listener != null) listener.onTabCloned(holder.getAdapterPosition()); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void moveItem(int from, int to) {
        if (from == to) return;
        Tab t = items.remove(from);
        items.add(to, t);
        notifyItemMoved(from, to);
    }

    public Tab removeAt(int pos) { Tab t = items.remove(pos); notifyItemRemoved(pos); return t; }

    public void addItem(Tab t, int at) { if (at < 0) items.add(t); else items.add(at, t); notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTitle, tvUrl;
        ImageButton btnClose, btnClone;
        VH(@NonNull View v) {
            super(v);
            ivThumb = v.findViewById(R.id.ivThumb);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvUrl = v.findViewById(R.id.tvUrl);
            btnClose = v.findViewById(R.id.btnCloseTab);
            btnClone = v.findViewById(R.id.btnCloneTab);
        }
    }
}
