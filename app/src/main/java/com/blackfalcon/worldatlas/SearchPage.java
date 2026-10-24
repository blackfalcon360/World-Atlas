package com.blackfalcon.worldatlas;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Offline search over countries, states/provinces and cities. */
final class SearchPage extends Page {
    private EditText input;
    private TextView count;
    private ListView list;
    private final Button[] chips = new Button[4];
    private int filter = 0;
    private List<Data.Hit> hits = new ArrayList<Data.Hit>();
    private boolean recentMode = true;
    private ResultAdapter adapter;

    SearchPage(MainActivity a, Ui u) {
        super(a, u);
    }

    @Override
    View build() {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);

        input = ui.input(ui.t("s_hint"));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { refresh(); }
        });
        box.addView(input, ui.gap());

        LinearLayout r = ui.row(box);
        String[] keys = {"s_all", "s_countries", "s_states", "s_cities"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            chips[i] = ui.btn(ui.t(keys[i]), Ui.GRAY, v -> {
                filter = idx;
                refresh();
            });
            chips[i].setTextSize(11);
            r.addView(chips[i], ui.weighted());
        }

        count = ui.tv("", 12, ui.sub);
        count.setPadding(ui.dp(4), ui.dp(2), ui.dp(4), ui.dp(4));
        box.addView(count, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        list = new ListView(act);
        list.setDivider(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        list.setDividerHeight(ui.dp(4));
        adapter = new ResultAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos >= 0 && pos < hits.size()) open(hits.get(pos));
        });
        box.addView(list, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        refresh();
        return box;
    }

    @Override
    void onShow() {
        if (recentMode) refresh();
    }

    private void open(Data.Hit h) {
        if (h.type == Data.Hit.COUNTRY) act.openCountry((Data.Country) h.ref);
        else if (h.type == Data.Hit.CITY) act.openCity((Data.City) h.ref);
        else act.openSub((Data.Sub) h.ref);
    }

    private void refresh() {
        if (input == null) return;
        for (int i = 0; i < 4; i++) chips[i].setBackground(ui.rounded(i == filter ? Ui.ORANGE : Ui.GRAY, 10));
        String q = input.getText().toString();
        if (Data.norm(q).isEmpty()) {
            recentMode = true;
            hits = recents();
            count.setText(hits.isEmpty() ? ui.t("s_help") : ui.t("s_recent"));
        } else {
            recentMode = false;
            hits = act.data.search(q, filter, 120);
            count.setText(hits.isEmpty() ? ui.t("s_none") : String.format(java.util.Locale.US, ui.t("s_count"), hits.size()));
        }
        adapter.notifyDataSetChanged();
    }

    private List<Data.Hit> recents() {
        List<Data.Hit> out = new ArrayList<Data.Hit>();
        for (String k : Store.recents(act)) {
            Data.Hit h = new Data.Hit();
            String body = k.length() > 2 ? k.substring(2) : "";
            if (k.startsWith("C:")) {
                Data.Country c = act.data.byIso.get(body);
                if (c == null) continue;
                h.type = Data.Hit.COUNTRY;
                h.ref = c;
            } else if (k.startsWith("S:")) {
                Data.Sub s = act.data.subByCode.get(body);
                if (s == null) continue;
                h.type = Data.Hit.SUB;
                h.ref = s;
            } else if (k.startsWith("T:")) {
                int bar = body.lastIndexOf('|');
                if (bar < 0) continue;
                Data.City found = null;
                java.util.List<Data.City> cs = act.data.citiesBy.get(body.substring(bar + 1));
                if (cs != null) {
                    for (Data.City c : cs) if (c.name.equals(body.substring(0, bar))) found = c;
                }
                if (found == null) continue;
                h.type = Data.Hit.CITY;
                h.ref = found;
            } else {
                continue;
            }
            out.add(h);
        }
        return out;
    }

    private static final class Holder {
        TextView icon, title, sub;
    }

    private final class ResultAdapter extends BaseAdapter {
        @Override public int getCount() { return hits.size(); }
        @Override public Object getItem(int i) { return hits.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            Holder h;
            if (cv == null) {
                LinearLayout r = new LinearLayout(act);
                r.setOrientation(LinearLayout.HORIZONTAL);
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(8));
                r.setBackground(ui.rounded(ui.card, 10));
                r.setLayoutParams(new AbsListView.LayoutParams(Ui.MATCH, Ui.WRAP));
                h = new Holder();
                h.icon = ui.tv("", 24, ui.tx);
                h.icon.setGravity(Gravity.CENTER);
                r.addView(h.icon, new LinearLayout.LayoutParams(ui.dp(44), Ui.WRAP));
                LinearLayout col = new LinearLayout(act);
                col.setOrientation(LinearLayout.VERTICAL);
                h.title = ui.tv("", 15, ui.tx);
                h.sub = ui.tv("", 12, ui.sub);
                col.addView(h.title);
                col.addView(h.sub);
                r.addView(col, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
                r.setTag(h);
                cv = r;
            } else {
                h = (Holder) cv.getTag();
            }
            Data.Hit hit = hits.get(pos);
            int lang = ui.lang();
            Data d = act.data;
            if (hit.type == Data.Hit.COUNTRY) {
                Data.Country c = (Data.Country) hit.ref;
                h.icon.setText(Geo.flag(c.iso2));
                h.title.setText(Data.name(c, lang));
                h.sub.setText(ui.t(c.sov ? "ty_country" : "ty_territory")
                        + (c.capital.equals("-") ? "" : "  \u2022  " + d.capitalName(c, lang)) + "  \u2022  " + Fmt.popM(c.popM, lang));
            } else if (hit.type == Data.Hit.CITY) {
                Data.City c = (Data.City) hit.ref;
                Data.Country k = d.byIso.get(c.iso);
                h.icon.setText(c.cap ? "\u2B50" : "\uD83C\uDFD9\uFE0F");
                h.title.setText(Data.name(c, lang));
                h.sub.setText((k == null ? "" : Data.name(k, lang) + "  \u2022  ") + Fmt.popK(c.popK, lang));
            } else {
                Data.Sub s = (Data.Sub) hit.ref;
                Data.Country k = d.byIso.get(s.country);
                h.icon.setText("\uD83D\uDDFA\uFE0F");
                h.title.setText(Data.name(s, lang));
                h.sub.setText(s.type + (k == null ? "" : "  \u2022  " + Geo.flag(k.iso2) + " " + Data.name(k, lang)));
            }
            return cv;
        }
    }
}
