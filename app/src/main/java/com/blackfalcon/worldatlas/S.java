package com.blackfalcon.worldatlas;

import android.content.Context;
import java.util.HashMap;

/** All UI text in English (0), Urdu (1), Arabic (2). */
final class S {
    private S() {}

    private static final HashMap<String, String[]> M = new HashMap<String, String[]>();

    private static void a(String k, String en, String ur, String ar) {
        M.put(k, new String[]{en, ur, ar});
    }

    static {
        a("app_name", "World Atlas", "عالمی اٹلس", "أطلس العالم");
        a("tab_map", "Map", "نقشہ", "الخريطة");
        a("tab_search", "Search", "تلاش", "بحث");
        a("tab_countries", "Countries", "ممالک", "الدول");
        a("tab_quiz", "Quiz", "کوئز", "مسابقة");
        a("tab_settings", "Settings", "سیٹنگز", "الإعدادات");
        a("by", "By: Black Falcon \uD83E\uDD85", "By: Black Falcon \uD83E\uDD85", "By: Black Falcon \uD83E\uDD85");
        a("back", "Back", "واپس", "رجوع");
        a("yes", "Yes", "ہاں", "نعم");
        a("no", "No", "نہیں", "لا");
        a("close", "Close", "بند کریں", "إغلاق");
        a("exit_title", "Exit", "باہر نکلیں", "خروج");
        a("exit_msg", "Do you want to exit the app?", "کیا آپ ایپ سے باہر نکلنا چاہتے ہیں؟", "هل تريد الخروج من التطبيق؟");
        a("loading", "Loading the atlas...", "اٹلس لوڈ ہو رہا ہے...", "جارٍ تحميل الأطلس...");
        a("error", "Something went wrong.", "کچھ غلط ہو گیا۔", "حدث خطأ ما.");
        a("copy", "Copy", "کاپی", "نسخ");
        a("copied", "Copied", "کاپی ہو گیا", "تم النسخ");
        // types
        a("ty_country", "Country", "ملک", "دولة");
        a("ty_territory", "Territory", "علاقہ", "إقليم");
        a("ty_city", "City", "شہر", "مدينة");
        a("ty_capital", "Capital city", "دارالحکومت", "عاصمة");
        a("ty_region", "Region", "خطہ", "منطقة");
        a("c_AF", "Africa", "افریقہ", "إفريقيا");
        a("c_AS", "Asia", "ایشیا", "آسيا");
        a("c_EU", "Europe", "یورپ", "أوروبا");
        a("c_NA", "North America", "شمالی امریکہ", "أمريكا الشمالية");
        a("c_SA", "South America", "جنوبی امریکہ", "أمريكا الجنوبية");
        a("c_OC", "Oceania", "اوشیانا", "أوقيانوسيا");
        a("c_AN", "Antarctica", "انٹارکٹیکا", "القارة القطبية الجنوبية");
        // detail
        a("d_capital", "Capital", "دارالحکومت", "العاصمة");
        a("d_continent", "Continent", "براعظم", "القارة");
        a("d_pop", "Population (approx.)", "آبادی (تقریباً)", "السكان (تقريبًا)");
        a("d_area", "Area", "رقبہ", "المساحة");
        a("d_density", "Density", "گنجانیت", "الكثافة");
        a("d_currency", "Currency", "کرنسی", "العملة");
        a("d_call", "Calling code", "کالنگ کوڈ", "رمز الاتصال");
        a("d_iso", "ISO codes", "ISO کوڈ", "رموز ISO");
        a("d_coords", "Coordinates", "کوآرڈینیٹس", "الإحداثيات");
        a("d_names", "Names", "نام", "الأسماء");
        a("d_subs", "States, provinces and regions", "ریاستیں، صوبے اور علاقے", "الولايات والمقاطعات والمناطق");
        a("d_cities", "Cities", "شہر", "المدن");
        a("d_nosubs", "No divisions in the ISO 3166-2 list for this country.", "اس ملک کے لیے ISO 3166-2 فہرست میں کوئی تقسیم نہیں۔", "لا توجد تقسيمات لهذه الدولة في قائمة ISO 3166-2.");
        a("d_map", "Show on map", "نقشے پر دکھائیں", "اعرض على الخريطة");
        a("d_wiki", "Wikipedia", "وکی پیڈیا", "ويكيبيديا");
        a("d_copy", "Copy details", "تفصیل کاپی کریں", "نسخ التفاصيل");
        a("d_me", "From your position", "آپ کے مقام سے", "من موقعك");
        a("d_country", "Country", "ملک", "الدولة");
        a("d_nearby", "Nearby cities", "قریبی شہر", "مدن قريبة");
        a("d_type", "Type", "قسم", "النوع");
        a("d_parent", "Part of", "حصہ ہے", "جزء من");
        a("d_code", "ISO 3166-2 code", "ISO 3166-2 کوڈ", "رمز ISO 3166-2");
        a("d_kids", "Subdivisions", "ذیلی تقسیمات", "التقسيمات الفرعية");
        a("d_nopos", "No map point is stored for this one, so the country is shown. Positions are approximate centers.", "اس کا نقشے پر کوئی نقطہ محفوظ نہیں، اس لیے ملک دکھایا جاتا ہے۔ مقامات تقریباً مراکز ہیں۔", "لا توجد نقطة محفوظة لها على الخريطة، لذا تُعرض الدولة. المواقع مراكز تقريبية.");
        a("d_approx", "Positions are approximate centers, not borders.", "مقامات تقریباً مراکز ہیں، سرحدیں نہیں۔", "المواقع مراكز تقريبية وليست حدودًا.");
        a("d_more", "more", "مزید", "المزيد");
        // map
        a("m_me", "My location", "میرا مقام", "موقعي");
        a("m_theme", "Map theme", "نقشے کی تھیم", "مظهر الخريطة");
        a("m_details", "Details", "تفصیل", "التفاصيل");
        a("m_nearest", "Nearest known place", "قریب ترین معلوم جگہ", "أقرب مكان معروف");
        a("m_perm", "Allow location to show your position on the map.", "نقشے پر اپنا مقام دیکھنے کے لیے لوکیشن کی اجازت دیں۔", "اسمح بالموقع لعرض موقعك على الخريطة.");
        a("m_nofix", "No position yet. Switch on GPS and wait outside.", "ابھی مقام نہیں ملا۔ GPS آن کریں اور باہر انتظار کریں۔", "لا يوجد موقع بعد. شغّل GPS وانتظر في الخارج.");
        a("m_point", "Point on map", "نقشے پر نقطہ", "نقطة على الخريطة");
        a("m_you", "You are here", "آپ یہاں ہیں", "أنت هنا");
        a("m_note", "Simplified coastlines. See Settings for how to add country borders.", "سادہ ساحلی خطوط۔ ملکی سرحدیں شامل کرنے کا طریقہ سیٹنگز میں ہے۔", "خطوط ساحلية مبسطة. راجع الإعدادات لإضافة حدود الدول.");
        // search
        a("s_hint", "Search country, city, state or province...", "ملک، شہر، ریاست یا صوبہ تلاش کریں...", "ابحث عن دولة أو مدينة أو ولاية أو مقاطعة...");
        a("s_all", "All", "سب", "الكل");
        a("s_countries", "Countries", "ممالک", "الدول");
        a("s_states", "States/provinces", "ریاستیں/صوبے", "الولايات/المقاطعات");
        a("s_cities", "Cities", "شہر", "المدن");
        a("s_none", "No results.", "کوئی نتیجہ نہیں۔", "لا نتائج.");
        a("s_count", "%d results", "%d نتائج", "%d نتيجة");
        a("s_recent", "Recently opened", "حال ہی میں کھولے گئے", "فُتحت مؤخرًا");
        a("s_help", "Type a name in English, Urdu or Arabic. Works offline.", "انگریزی، اردو یا عربی میں نام لکھیں۔ آف لائن کام کرتا ہے۔", "اكتب اسمًا بالإنجليزية أو الأردية أو العربية. يعمل بدون إنترنت.");
        // countries
        a("co_name", "Name", "نام", "الاسم");
        a("co_pop", "Population", "آبادی", "السكان");
        a("co_area", "Area", "رقبہ", "المساحة");
        a("co_all", "All", "سب", "الكل");
        a("co_terr", "Territories", "علاقے", "الأقاليم");
        a("co_count", "%d entries", "%d اندراجات", "%d إدخال");
        // quiz
        a("q_flags", "Guess the flag", "جھنڈا پہچانیں", "خمّن العلم");
        a("q_capitals", "Capital of the country", "ملک کا دارالحکومت", "عاصمة الدولة");
        a("q_countries", "Country of the capital", "دارالحکومت کا ملک", "دولة العاصمة");
        a("q_currencies", "Currency of the country", "ملک کی کرنسی", "عملة الدولة");
        a("q_best", "Best", "بہترین", "الأفضل");
        a("q_score", "Score", "سکور", "النتيجة");
        a("q_q", "Question %d of %d", "سوال %d از %d", "السؤال %d من %d");
        a("q_next", "Next", "اگلا", "التالي");
        a("q_again", "Play again", "دوبارہ کھیلیں", "العب مرة أخرى");
        a("q_menu", "Menu", "مینو", "القائمة");
        a("q_right", "Correct!", "درست!", "صحيح!");
        a("q_wrong", "Wrong. Correct answer:", "غلط۔ درست جواب:", "خطأ. الإجابة الصحيحة:");
        a("q_result", "Your score", "آپ کا سکور", "نتيجتك");
        a("q_new_best", "New best score!", "نیا بہترین سکور!", "رقم قياسي جديد!");
        a("q_note", "Flags are shown as emoji, so they depend on your phone's font.", "جھنڈے ایموجی کے طور پر دکھائے جاتے ہیں، اس لیے آپ کے فون کے فونٹ پر منحصر ہیں۔", "تُعرض الأعلام كرموز تعبيرية لذا تعتمد على خط هاتفك.");
        // settings
        a("st_lang", "Language", "زبان", "اللغة");
        a("st_theme", "App theme", "ایپ تھیم", "مظهر التطبيق");
        a("st_dark", "Dark", "ڈارک", "داكن");
        a("st_light", "Light", "لائٹ", "فاتح");
        a("st_map", "Map theme", "نقشے کی تھیم", "مظهر الخريطة");
        a("mt_0", "Night", "رات", "ليلي");
        a("mt_1", "Day", "دن", "نهاري");
        a("mt_2", "Terrain", "زمین", "تضاريس");
        a("mt_3", "Paper", "کاغذ", "ورقي");
        a("st_units", "Distance units", "فاصلے کی اکائی", "وحدة المسافة");
        a("st_km", "Kilometers", "کلومیٹر", "كيلومتر");
        a("st_mi", "Miles", "میل", "ميل");
        a("st_save", "Save", "محفوظ کریں", "حفظ");
        a("st_saved", "Saved", "محفوظ ہو گیا", "تم الحفظ");
        a("st_about", "About, data and limits", "تعارف، ڈیٹا اور حدود", "حول البيانات والقيود");
        a("st_about_text", "Everything works offline. The app has no internet permission; only the Wikipedia button opens your browser, and only when you tap it.\n\nData: country names and ISO codes (ISO 3166, iso-codes project), Urdu and Arabic country, currency and many city names (Unicode CLDR), representative places (IANA tz database), plus capitals, populations, areas, city positions and region centers compiled by hand and rounded. Populations are approximate.\n\nLimits: coastlines are simplified and there are no country borders in the built-in map; the optional borders file (see README) adds them. Many regions have a stored map point, but not all: the others open with the country shown. City, state and province names are in English except where Urdu or Arabic names are available. Search is by name, not by address.", "سب کچھ آف لائن چلتا ہے۔ ایپ کے پاس انٹرنیٹ کی اجازت نہیں؛ صرف وکی پیڈیا کا بٹن آپ کا براؤزر کھولتا ہے، اور وہ بھی آپ کے دبانے پر۔\n\nڈیٹا: ممالک کے نام اور ISO کوڈ (ISO 3166، iso-codes پروجیکٹ)، اردو اور عربی میں ممالک، کرنسیوں اور کئی شہروں کے نام (Unicode CLDR)، نمائندہ مقامات (IANA tz ڈیٹابیس)، اور ہاتھ سے مرتب کردہ دارالحکومت، آبادیاں، رقبے، شہروں کے مقامات اور علاقوں کے مراکز۔ آبادیاں تقریبی ہیں۔\n\nحدود: ساحلی خطوط سادہ ہیں اور بلٹ اِن نقشے میں ملکی سرحدیں نہیں؛ اختیاری سرحدوں کی فائل (README دیکھیں) انہیں شامل کرتی ہے۔ کئی علاقوں کا نقشے پر نقطہ محفوظ ہے مگر سب کا نہیں: باقی میں ملک دکھایا جاتا ہے۔ شہروں، ریاستوں اور صوبوں کے نام انگریزی میں ہیں سوائے جہاں اردو یا عربی نام موجود ہیں۔ تلاش نام سے ہے، پتے سے نہیں۔", "كل شيء يعمل بدون إنترنت. لا يملك التطبيق إذن الإنترنت؛ فقط زر ويكيبيديا يفتح متصفحك وعند ضغطك عليه فقط.\n\nالبيانات: أسماء الدول ورموز ISO (ISO 3166 ومشروع iso-codes) وأسماء الدول والعملات والعديد من المدن بالأردية والعربية (Unicode CLDR) وأماكن تمثيلية (قاعدة IANA tz) إضافة إلى العواصم والسكان والمساحات ومواقع المدن ومراكز المناطق المجمّعة يدويًا والمقرّبة. أعداد السكان تقريبية.\n\nالقيود: الخطوط الساحلية مبسطة ولا توجد حدود دول في الخريطة المدمجة؛ ملف الحدود الاختياري (راجع README) يضيفها. لكثير من المناطق نقطة محفوظة على الخريطة وليس لكلها: وتُعرض الدولة للباقي. أسماء المدن والولايات والمقاطعات بالإنجليزية إلا حيث تتوفر أسماء أردية أو عربية. البحث بالاسم وليس بالعنوان.");
    }

    static String t(Context c, String key) {
        String[] v = M.get(key);
        if (v == null) return key;
        return v[Store.langIdx(c)];
    }
}
