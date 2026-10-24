package com.blackfalcon.worldatlas;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends Activity {

    Data data;
    Ui ui;

    private FrameLayout content;
    private final Button[] tabB = new Button[5];
    private final Page[] tabs = new Page[5];
    private final View[] tabViews = new View[5];
    private final ArrayList<Page> stack = new ArrayList<Page>();
    private int tab = 0;
    private boolean paused = false;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new Ui(this);
        buildShell();
        TextView wait = ui.tv(ui.t("loading"), 16, ui.sub);
        wait.setGravity(Gravity.CENTER);
        content.addView(wait, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        new Thread(() -> {
            Data d = null;
            try {
                d = Data.load(new Data.Source() {
                    @Override
                    public InputStream open(String name) throws IOException {
                        return getAssets().open(name);
                    }
                });
            } catch (Exception e) {
                d = null;
            }
            final Data res = d;
            runOnUiThread(() -> {
                if (res == null) {
                    Toast.makeText(MainActivity.this, ui.t("error"), Toast.LENGTH_LONG).show();
                    return;
                }
                data = res;
                loaded = true;
                initTabs();
                render();
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        Page p = current();
        if (p != null) p.onShow();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
        Page p = current();
        if (p != null) p.onHide();
    }

    @Override
    public void onBackPressed() {
        if (!stack.isEmpty()) {
            pop();
            return;
        }
        ui.dlg(this).setTitle(ui.t("exit_title")).setMessage(ui.t("exit_msg"))
                .setPositiveButton(ui.t("yes"), (d, w) -> finish())
                .setNegativeButton(ui.t("no"), null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 77) {
            Page p = current();
            if (p != null) p.onPermission();
        }
    }

    void requestLocation() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, 77);
    }

    // ---------- small services for pages ----------

    int lang() { return ui.lang(); }

    boolean miles() { return Store.miles(this); }

    double[] myPos() {
        if (Gps.I.last == null) return null;
        return new double[]{Gps.I.last.getLatitude(), Gps.I.last.getLongitude()};
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    void copy(String s) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("atlas", s));
        toast(ui.t("copied"));
    }

    private void browse(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast(ui.t("error"));
        }
    }

    private static String enc(String title) {
        try {
            return URLEncoder.encode(title.replace(' ', '_'), "UTF-8");
        } catch (Exception e) {
            return title;
        }
    }

    /** Opens the article in the app language when a name in that language exists, else English. */
    void openWiki(String en, String ur, String ar) {
        int l = lang();
        if (l == 1 && ur != null && !ur.isEmpty()) browse("https://ur.wikipedia.org/wiki/" + enc(ur));
        else if (l == 2 && ar != null && !ar.isEmpty()) browse("https://ar.wikipedia.org/wiki/" + enc(ar));
        else browse("https://en.wikipedia.org/wiki/" + enc(en));
    }

    void openWikiSearch(String query) {
        try {
            browse("https://en.wikipedia.org/w/index.php?search=" + URLEncoder.encode(query, "UTF-8"));
        } catch (Exception e) {
            toast(ui.t("error"));
        }
    }

    // ---------- layout ----------

    private void buildShell() {
        ui.refresh();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ui.bg);
        root.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(6));
        root.setLayoutDirection("en".equals(Store.lang(this))
                ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        LinearLayout bar = ui.row(root);
        String[] keys = {"tab_map", "tab_search", "tab_countries", "tab_quiz", "tab_settings"};
        String[] icons = {"\uD83D\uDDFA\uFE0F ", "\uD83D\uDD0E ", "\uD83C\uDF0D ", "\u2753 ", "\u2699\uFE0F "};
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            tabB[i] = ui.btn(icons[i] + ui.t(keys[i]), Ui.GRAY, v -> {
                if (!loaded) return;
                clearStack();
                tab = idx;
                render();
            });
            tabB[i].setTextSize(10);
            tabB[i].setSingleLine(true);
            tabB[i].setPadding(ui.dp(2), ui.dp(6), ui.dp(2), ui.dp(6));
            bar.addView(tabB[i], ui.weighted());
        }

        TextView by = ui.tv(ui.t("by"), 13, ui.sub);
        by.setGravity(Gravity.CENTER);
        by.setPadding(0, ui.dp(4), 0, 0);
        root.addView(by, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        setContentView(root);
    }

    private void initTabs() {
        tabs[0] = new MapPage(this, ui);
        tabs[1] = new SearchPage(this, ui);
        tabs[2] = new CountriesPage(this, ui);
        tabs[3] = new QuizPage(this, ui);
        tabs[4] = new SettingsPage(this, ui);
        for (int i = 0; i < 5; i++) tabViews[i] = null;
    }

    private Page current() {
        if (!loaded) return null;
        if (!stack.isEmpty()) return stack.get(stack.size() - 1);
        return tabs[tab];
    }

    private void clearStack() {
        Page p = current();
        if (p != null) p.onHide();
        stack.clear();
    }

    private void render() {
        if (!loaded) return;
        content.removeAllViews();
        for (int i = 0; i < 5; i++) {
            tabB[i].setBackground(ui.rounded(i == tab ? Ui.ORANGE : Ui.GRAY, 10));
        }
        if (stack.isEmpty()) {
            if (tabViews[tab] == null) tabViews[tab] = tabs[tab].build();
            View v = tabViews[tab];
            if (v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
            content.addView(v, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            if (!paused) tabs[tab].onShow();
            return;
        }
        Page p = stack.get(stack.size() - 1);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = ui.row(page);
        bar.addView(ui.btn(ui.t("back"), Ui.GRAY, v -> pop()), new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
        TextView h = ui.tv(p.title(), 17, ui.tx);
        h.setPadding(ui.dp(10), 0, ui.dp(10), 0);
        h.setSingleLine(true);
        bar.addView(h, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        ScrollView sv = new ScrollView(this);
        sv.addView(p.build(), new ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));
        page.addView(sv, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        content.addView(page, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        if (!paused) p.onShow();
    }

    private void push(Page p) {
        Page cur = current();
        if (cur != null) cur.onHide();
        stack.add(p);
        render();
    }

    private void pop() {
        if (stack.isEmpty()) return;
        Page p = stack.remove(stack.size() - 1);
        p.onHide();
        render();
    }

    void rebuildAll() {
        Page cur = current();
        if (cur != null) cur.onHide();
        stack.clear();
        buildShell();
        initTabs();
        tab = 4;
        render();
    }

    // ---------- navigation ----------

    void openCountry(Data.Country c) {
        Store.addRecent(this, "C:" + c.iso2);
        push(new CountryPage(this, ui, c));
    }

    void openCity(Data.City c) {
        Store.addRecent(this, "T:" + c.name + "|" + c.iso);
        push(new CityPage(this, ui, c));
    }

    void openSub(Data.Sub s) {
        Store.addRecent(this, "S:" + s.code);
        push(new SubPage(this, ui, s));
    }

    private void goMap(double lat, double lon, float sd, String label, Object ref, String iso) {
        clearStack();
        tab = 0;
        render();
        ((MapPage) tabs[0]).focusOn(lat, lon, sd, label, ref, iso);
    }

    void showCountryOnMap(Data.Country c) {
        float sd;
        if (c.area > 5e6) sd = 3f;
        else if (c.area > 1e6) sd = 5f;
        else if (c.area > 2e5) sd = 8f;
        else if (c.area > 5e4) sd = 12f;
        else if (c.area > 1e4) sd = 20f;
        else sd = 40f;
        goMap(c.lat, c.lon, sd, Data.name(c, lang()), c, c.iso2);
    }

    void showCityOnMap(Data.City c) {
        goMap(c.lat, c.lon, 30f, Data.name(c, lang()), c, c.iso);
    }

    void showSubOnMap(Data.Sub s) {
        Data.Country k = data.byIso.get(s.country);
        if (s.hasPos()) {
            goMap(s.lat, s.lon, s.parent.isEmpty() ? 12f : 22f, Data.name(s, lang()), s, s.country);
        } else if (k != null) {
            goMap(k.lat, k.lon, 6f, Data.name(s, lang()) + " (" + Data.name(k, lang()) + ")", k, s.country);
        }
    }
}
