package com.blackfalcon.worldatlas;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;
import java.util.Random;

/** Four quiz modes with 10 questions each. Best scores are stored on the phone. */
final class QuizPage extends Page {
    private static final int TOTAL = 10;
    private FrameLayout frame;
    private QuizEngine engine;
    private int mode = -1;
    private int qNo = 0;
    private int score = 0;
    private QuizEngine.Question q;
    private boolean answered = false;

    QuizPage(MainActivity a, Ui u) {
        super(a, u);
    }

    @Override
    View build() {
        frame = new FrameLayout(act);
        engine = new QuizEngine(act.data, new Random());
        showMenu();
        return frame;
    }

    private void set(View v) {
        frame.removeAllViews();
        ScrollView sv = new ScrollView(act);
        sv.addView(v, new android.view.ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));
        frame.addView(sv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
    }

    private void showMenu() {
        mode = -1;
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        String[] keys = {"q_flags", "q_capitals", "q_countries", "q_currencies"};
        String[] icons = {"\uD83C\uDFF3\uFE0F", "\uD83C\uDFDB\uFE0F", "\uD83C\uDF0D", "\uD83D\uDCB0"};
        for (int i = 0; i < 4; i++) {
            final int m = i;
            LinearLayout c = ui.card();
            LinearLayout r = ui.row(c);
            TextView ic = ui.tv(icons[i], 30, ui.tx);
            ic.setPadding(0, 0, ui.dp(12), 0);
            r.addView(ic, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            LinearLayout col = new LinearLayout(act);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(ui.tv(ui.t(keys[i]), 17, ui.tx));
            col.addView(ui.tv(ui.t("q_best") + ": " + Store.best(act, i) + "/" + TOTAL, 12, ui.sub));
            r.addView(col, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            c.setOnClickListener(v -> start(m));
            box.addView(c, ui.gap());
        }
        TextView note = ui.tv(ui.t("q_note"), 12, ui.sub);
        note.setPadding(ui.dp(4), 0, ui.dp(4), 0);
        box.addView(note);
        set(box);
    }

    private void start(int m) {
        mode = m;
        qNo = 0;
        score = 0;
        nextQuestion();
    }

    private void nextQuestion() {
        if (qNo >= TOTAL) {
            showResult();
            return;
        }
        q = engine.next(mode, ui.lang());
        qNo++;
        answered = false;
        showQuestion();
    }

    private void showQuestion() {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = ui.row(box);
        head.addView(ui.tv(String.format(Locale.US, ui.t("q_q"), qNo, TOTAL), 13, ui.sub),
                new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        head.addView(ui.tv(ui.t("q_score") + ": " + score, 13, ui.tx), new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));

        LinearLayout pc = ui.card();
        TextView prompt = ui.tv(q.prompt, mode == QuizEngine.FLAG ? 88 : 30, ui.tx);
        prompt.setGravity(Gravity.CENTER);
        pc.addView(prompt, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        LinearLayout.LayoutParams pp = ui.gap();
        pp.setMargins(0, ui.dp(8), 0, ui.dp(10));
        box.addView(pc, pp);

        final Button[] bs = new Button[4];
        final TextView fb = ui.tv("", 15, ui.tx);
        final Button next = ui.btn(ui.t("q_next"), Ui.GREEN, v -> nextQuestion());
        next.setVisibility(View.INVISIBLE);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            bs[i] = ui.btn(q.options[i], Ui.GRAY, v -> {
                if (answered) return;
                answered = true;
                boolean ok = idx == q.correct;
                if (ok) score++;
                bs[idx].setBackground(ui.rounded(ok ? Ui.GREEN : Ui.RED, 10));
                bs[q.correct].setBackground(ui.rounded(Ui.GREEN, 10));
                fb.setText(ok ? ui.t("q_right") : ui.t("q_wrong") + " " + q.options[q.correct]);
                next.setVisibility(View.VISIBLE);
            });
            bs[i].setTextSize(16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, ui.dp(52));
            lp.setMargins(0, 0, 0, ui.dp(8));
            box.addView(bs[i], lp);
        }
        fb.setGravity(Gravity.CENTER);
        box.addView(fb, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        box.addView(next, new LinearLayout.LayoutParams(Ui.MATCH, ui.dp(50)));
        set(box);
    }

    private void showResult() {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout c = ui.card();
        TextView t = ui.tv(ui.t("q_result"), 16, ui.sub);
        t.setGravity(Gravity.CENTER);
        TextView s = ui.tv(score + " / " + TOTAL, 48, ui.tx);
        s.setGravity(Gravity.CENTER);
        c.addView(t, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        c.addView(s, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        if (score > Store.best(act, mode)) {
            Store.setBest(act, mode, score);
            TextView nb = ui.tv(ui.t("q_new_best"), 15, 0xFFFFC857);
            nb.setGravity(Gravity.CENTER);
            c.addView(nb, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }
        box.addView(c, ui.gap());
        final int m = mode;
        LinearLayout r = ui.row(box);
        r.addView(ui.btn(ui.t("q_again"), Ui.GREEN, v -> start(m)), ui.weighted());
        r.addView(ui.btn(ui.t("q_menu"), Ui.GRAY, v -> showMenu()), ui.weighted());
        set(box);
    }
}
