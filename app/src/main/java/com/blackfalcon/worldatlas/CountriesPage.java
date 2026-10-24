package com.blackfalcon.worldatlas;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Browse all countries and territories, sort and filter by continent. */
final class CountriesPage extends Page {
    private static final String[] FILTERS = {"ALL", "AS", "EU", "AF", "NA", "SA", "OC", "AN", "TERR"};
    private int filter = 0;
    private int sort = 0; // 0 name, 1 population, 2 area
    private List<Data.Country> shown = new ArrayList<Data.Country>();
    private final Button[] sortB = new Button[3];
    private final Button[] chipB = new Button[FILTERS.length];
    private TextView count;
    private BaseAdapter adapter;

    CountriesPage(MainActivity a, Ui u) {
        super(a, u);
    }

    @Override
    View build() {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout r = ui.row(box);
        String[] sk = {"co_name", "co_pop", "co_area"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            sortB[i] = ui.btn(ui.t(sk[i]), Ui.GRAY, v -> {
                sort = idx;
                refresh();
            });
            r.addView(sortB[i], ui.weighted());
        }

        HorizontalScrollView hs = new HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(act);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < FILTERS.length; i++) {
            final int idx = i;
            String label = FILTERS[i].equals("ALL") ? ui.t("co_all") : (FILTERS[i].equals("TERR") ? ui.t("co_terr") : ui.t("c_" + FILTERS[i]));
            chipB[i] = ui.btn(label, Ui.GRAY, v -> {
                filter = idx;
                refresh();
            });
            chipB[i].setTextSize(12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            lp.setMargins(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3));
            chips.addView(chipB[i], lp);
        }
        hs.addView(chips);
        box.addView(hs, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        count = ui.tv("", 12, ui.sub);
        count.setPadding(ui.dp(4), ui.dp(2), ui.dp(4), ui.dp(4));
        box.addView(count, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        ListView list = new ListView(act);
        list.setDivider(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        list.setDividerHeight(ui.dp(4));
        adapter = new Adapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos >= 0 && pos < shown.size()) act.openCountry(shown.get(pos));
        });
        box.addView(list, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        refresh();
        return box;
    }

    private void refresh() {
        for (int i = 0; i < 3; i++) sortB[i].setBackground(ui.rounded(i == sort ? Ui.ORANGE : Ui.GRAY, 10));
        for (int i = 0; i < FILTERS.length; i++) chipB[i].setBackground(ui.rounded(i == filter ? Ui.ORANGE : Ui.GRAY, 10));
        final int lang = ui.lang();
        String f = FILTERS[filter];
        List<Data.Country> l = new ArrayList<Data.Country>();
        for (Data.Country c : act.data.countries) {
            if (f.equals("ALL") || (f.equals("TERR") ? !c.sov : c.cont.equals(f))) l.add(c);
        }
        final int s = sort;
        Collections.sort(l, new Comparator<Data.Country>() {
            @Override
            public int compare(Data.Country a, Data.Country b) {
                if (s == 1) return Double.compare(b.popM, a.popM);
                if (s == 2) return Double.compare(b.area, a.area);
                return Data.name(a, lang).compareToIgnoreCase(Data.name(b, lang));
            }
        });
        shown = l;
        count.setText(String.format(Locale.US, ui.t("co_count"), l.size()));
        adapter.notifyDataSetChanged();
    }

    private static final class Holder {
        TextView icon, title, sub;
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
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
                h.icon = ui.tv("", 28, ui.tx);
                h.icon.setGravity(Gravity.CENTER);
                r.addView(h.icon, new LinearLayout.LayoutParams(ui.dp(48), Ui.WRAP));
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
            Data.Country c = shown.get(pos);
            int lang = ui.lang();
            h.icon.setText(Geo.flag(c.iso2));
            h.title.setText(Data.name(c, lang));
            String second = sort == 2 ? Fmt.area(c.area, act.miles()) : Fmt.popM(c.popM, lang);
            h.sub.setText((c.capital.equals("-") ? "" : act.data.capitalName(c, lang) + "  \u2022  ") + second
                    + (c.sov ? "" : "  \u2022  " + ui.t("ty_territory")));
            return cv;
        }
    }
}
