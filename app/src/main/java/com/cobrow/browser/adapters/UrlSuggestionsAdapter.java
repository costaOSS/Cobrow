package com.cobrow.browser.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.cobrow.browser.R;
import com.cobrow.browser.data.Bookmark;
import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.HistoryItem;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class UrlSuggestionsAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private List<Suggestion> suggestions = new ArrayList<>();
    private final CobrowDatabase db;

    public UrlSuggestionsAdapter(Context context) {
        this.context = context;
        this.db = CobrowDatabase.get(context);
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Override
    public Suggestion getItem(int position) {
        return suggestions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        Suggestion s = getItem(position);
        TextView text1 = convertView.findViewById(android.R.id.text1);
        TextView text2 = convertView.findViewById(android.R.id.text2);

        text1.setText(s.title);
        text2.setText(s.url);
        
        if (s.type == SuggestionType.SEARCH) {
            text2.setVisibility(View.GONE);
        } else {
            text2.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null && constraint.length() > 0) {
                    List<Suggestion> list = new ArrayList<>();
                    String q = constraint.toString();

                    // 1. Bookmarks
                    List<Bookmark> bookmarks = db.bookmarkDao().search(q);
                    for (Bookmark b : bookmarks) {
                        list.add(new Suggestion(b.title, b.url, SuggestionType.BOOKMARK));
                    }

                    // 2. History
                    List<HistoryItem> history = db.historyDao().search(q);
                    for (HistoryItem h : history) {
                        // Avoid duplicates if already in bookmarks
                        boolean exists = false;
                        for (Suggestion s : list) {
                            if (s.url.equals(h.url)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            list.add(new Suggestion(h.title, h.url, SuggestionType.HISTORY));
                        }
                    }

                    // 3. Search suggestions (DuckDuckGo)
                    try {
                        String urlStr = "https://duckduckgo.com/ac/?q=" + URLEncoder.encode(q, "UTF-8") + "&type=list";
                        URL url = new URL(urlStr);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = rd.readLine()) != null) {
                            sb.append(line);
                        }
                        rd.close();
                        
                        // DDG returns [query, ["suggestion1", "suggestion2", ...]]
                        JSONArray json = new JSONArray(sb.toString());
                        if (json.length() > 1) {
                            JSONArray array = json.getJSONArray(1);
                            for (int i = 0; i < Math.min(array.length(), 5); i++) {
                                String suggestion = array.getString(i);
                                list.add(new Suggestion(suggestion, suggestion, SuggestionType.SEARCH));
                            }
                        }
                    } catch (Exception ignored) {}

                    results.values = list;
                    results.count = list.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    suggestions = (List<Suggestion>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return ((Suggestion) resultValue).url;
            }
        };
    }

    public enum SuggestionType {
        HISTORY, BOOKMARK, SEARCH
    }

    public static class Suggestion {
        public String title;
        public String url;
        public SuggestionType type;

        public Suggestion(String title, String url, SuggestionType type) {
            this.title = title;
            this.url = url;
            this.type = type;
        }
    }
}
