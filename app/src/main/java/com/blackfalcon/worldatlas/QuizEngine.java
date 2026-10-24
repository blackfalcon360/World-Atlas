package com.blackfalcon.worldatlas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Builds multiple-choice questions. Pure Java. */
final class QuizEngine {
    static final int FLAG = 0, CAPITAL_OF = 1, COUNTRY_OF_CAPITAL = 2, CURRENCY = 3;

    static final class Question {
        String prompt;      // big text (flag emoji or name)
        String[] options = new String[4];
        int correct;
        Data.Country answer;
    }

    private final Data data;
    private final Random rnd;

    QuizEngine(Data d, Random r) {
        data = d;
        rnd = r;
    }

    private List<Data.Country> pool(int mode) {
        List<Data.Country> l = new ArrayList<Data.Country>();
        for (Data.Country c : data.countries) {
            if (!c.sov) continue;
            if (mode == CAPITAL_OF || mode == COUNTRY_OF_CAPITAL) {
                if (c.capital.equals("-")) continue;
            }
            if (mode == CURRENCY && (c.curCode.isEmpty() || c.curEn.isEmpty())) continue;
            l.add(c);
        }
        return l;
    }

    private String text(Data.Country c, int mode, int lang) {
        switch (mode) {
            case FLAG: return Data.name(c, lang);
            case CAPITAL_OF: return data.capitalName(c, lang);
            case COUNTRY_OF_CAPITAL: return Data.name(c, lang);
            default: return Data.currency(c, lang) + " (" + c.curCode + ")";
        }
    }

    Question next(int mode, int lang) {
        List<Data.Country> pool = pool(mode);
        Question q = new Question();
        Data.Country ans = pool.get(rnd.nextInt(pool.size()));
        q.answer = ans;
        switch (mode) {
            case FLAG: q.prompt = Geo.flag(ans.iso2); break;
            case CAPITAL_OF: q.prompt = Data.name(ans, lang); break;
            case COUNTRY_OF_CAPITAL: q.prompt = data.capitalName(ans, lang); break;
            default: q.prompt = Data.name(ans, lang); break;
        }
        List<String> opts = new ArrayList<String>();
        HashSet<String> seen = new HashSet<String>();
        String right = text(ans, mode, lang);
        opts.add(right);
        seen.add(right);
        int guard = 0;
        while (opts.size() < 4 && guard++ < 2000) {
            Data.Country c = pool.get(rnd.nextInt(pool.size()));
            if (c == ans) continue;
            String t = text(c, mode, lang);
            if (seen.add(t)) opts.add(t);
        }
        Collections.shuffle(opts, rnd);
        for (int i = 0; i < 4; i++) q.options[i] = opts.get(i);
        q.correct = opts.indexOf(right);
        return q;
    }
}
