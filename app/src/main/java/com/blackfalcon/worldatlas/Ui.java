package com.blackfalcon.worldatlas;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers to build the UI from code. */
final class Ui {
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static final int GRAY = 0xFF455A64;
    static final int RED = 0xFFC62828;
    static final int GREEN = 0xFF2E7D32;
    static final int BLUE = 0xFF1565C0;
    static final int ORANGE = 0xFFEF6C00;
    static final int TEAL = 0xFF00796B;

    final Context c;
    boolean dark;
    int bg, card, tx, sub;

    Ui(Context c) {
        this.c = c;
        refresh();
    }

    void refresh() {
        dark = Store.dark(c);
        bg = dark ? Color.BLACK : 0xFFF2F2F2;
        card = dark ? 0xFF1C1C1E : Color.WHITE;
        tx = dark ? Color.WHITE : 0xFF111111;
        sub = dark ? 0xFFAAAAAA : 0xFF555555;
    }

    String t(String k) { return S.t(c, k); }

    int lang() { return Store.langIdx(c); }

    int dp(int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    GradientDrawable rounded(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    AlertDialog.Builder dlg(Context ctx) {
        return new AlertDialog.Builder(ctx, dark
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
    }

    TextView tv(String s, int sp, int color) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    Button btn(String text, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setBackground(rounded(color, 10));
        b.setPadding(dp(6), dp(6), dp(6), dp(6));
        b.setOnClickListener(l);
        return b;
    }

    LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, WRAP, 1f);
        p.setMargins(dp(3), dp(3), dp(3), dp(3));
        return p;
    }

    /** Horizontal row, already added to parent with MATCH_PARENT width (children may use weight). */
    LinearLayout row(LinearLayout parent) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(r, new LinearLayout.LayoutParams(MATCH, WRAP));
        return r;
    }

    LinearLayout card() {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(10), dp(10), dp(10), dp(10));
        v.setBackground(rounded(card, 12));
        return v;
    }

    LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.setMargins(0, 0, 0, dp(8));
        return p;
    }

    /** Text box: black text on white background in both themes. */
    EditText input(String hint) {
        EditText e = new EditText(c);
        e.setSingleLine(true);
        e.setTextColor(Color.BLACK);
        e.setHintTextColor(0xFF777777);
        e.setHint(hint);
        e.setBackground(rounded(Color.WHITE, 10));
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        return e;
    }

    void tint(CompoundButton b) {
        b.setTextColor(tx);
        b.setButtonTintList(ColorStateList.valueOf(tx));
    }
}
