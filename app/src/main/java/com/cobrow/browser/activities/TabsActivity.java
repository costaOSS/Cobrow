package com.cobrow.browser.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.cobrow.browser.R;
import com.cobrow.browser.adapters.TabsGridAdapter;
import com.cobrow.browser.data.Tab;
import com.cobrow.browser.data.TabsManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class TabsActivity extends AppCompatActivity implements TabsGridAdapter.Listener {

    private RecyclerView rvTabs;
    private FloatingActionButton fabAdd;
    private TabsManager tabsManager;
    private TabsGridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs_grid);

        tabsManager = new TabsManager(this);
        rvTabs = findViewById(R.id.rvTabs);
        fabAdd = findViewById(R.id.fabAddTab);

        adapter = new TabsGridAdapter(this);
        rvTabs.setAdapter(adapter);

        int span = calculateSpanCount();
        rvTabs.setLayoutManager(new GridLayoutManager(this, span));

        // ItemTouchHelper for drag & drop + swipe to dismiss
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                adapter.moveItem(from, to);
                tabsManager.saveTabs(adapter.getItems());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                new AlertDialog.Builder(TabsActivity.this)
                        .setTitle("Close tab")
                        .setMessage("Close this tab?")
                        .setPositiveButton("Close", (d, w) -> {
                            tabsManager.removeTab(pos);
                            refresh();
                        })
                        .setNegativeButton("Cancel", (d, w) -> refresh())
                        .show();
            }
        };
        ItemTouchHelper ith = new ItemTouchHelper(callback);
        ith.attachToRecyclerView(rvTabs);

        refresh();

        fabAdd.setOnClickListener(v -> {
            tabsManager.addTab(new Tab("", tabsManager.getTabs().isEmpty() ? "https://www.google.com" : tabsManager.getTabs().get(tabsManager.getTabs().size() - 1).url));
            refresh();
        });
    }

    private int calculateSpanCount() {
        // Prefer 4 columns for tablets (sw600dp) else 2 columns on phones
        boolean tablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        return tablet ? 4 : 2;
    }

    private void refresh() {
        List<Tab> tabs = tabsManager.getTabs();
        adapter.setItems(tabs);
    }

    @Override
    public void onTabClicked(int position) {
        Intent data = new Intent();
        data.putExtra("selected_index", position);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onTabClosed(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Close tab")
                .setMessage("Close this tab?")
                .setPositiveButton("Close", (d, w) -> {
                    tabsManager.removeTab(position);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onTabCloned(int position) {
        List<Tab> tabs = tabsManager.getTabs();
        if (position >= 0 && position < tabs.size()) {
            Tab t = tabs.get(position);
            tabsManager.addTab(new Tab(t.title, t.url, t.thumbnail, t.incognito));
            refresh();
        }
    }
}
