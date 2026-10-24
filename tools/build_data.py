#!/usr/bin/env python3
"""Builds the offline data files in app/src/main/assets from:
 - data_src/*.txt (hand-compiled: capitals, cities, subdivision centers, Urdu/Arabic names)
 - iso-codes (ISO 3166-1/-2 names), Unicode CLDR via Babel (country, currency and city names), tz database (zone.tab)
Run: python3 tools/build_data.py <data_src dir> <assets dir>
"""
import json, math, re, sys, unicodedata, os, subprocess
from babel import Locale
from babel.numbers import get_territory_currencies

SRC, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
en, ur, ar = Locale('en'), Locale('ur'), Locale('ar')
iso1 = json.load(open('/usr/share/iso-codes/json/iso_3166-1.json'))['3166-1']
iso2 = json.load(open('/usr/share/iso-codes/json/iso_3166-2.json'))['3166-2']
alpha3 = {x['alpha_2']: x['alpha_3'] for x in iso1}
alpha3['XK'] = 'XKX'

def norm(s):
    return ''.join(ch for ch in unicodedata.normalize('NFKD', s.lower()) if ch.isalnum())

def hav(a, b, c, d):
    p = math.pi / 180
    x = math.sin((c - a) * p / 2) ** 2 + math.cos(a * p) * math.cos(c * p) * math.sin((d - b) * p / 2) ** 2
    return 6371 * 2 * math.asin(math.sqrt(x))

def lines(fn):
    for l in open(os.path.join(SRC, fn), encoding='utf8'):
        l = l.rstrip('\n')
        if l.strip() and not l.startswith('#'):
            yield l

# territories (not independent states) -> sov = 0
TERR = set("AW AI AX AS AQ TF BQ BL BM BV CC CK CW CX KY EH FK FO GG GI GP GL GF GU HK HM IM IO JE MO MF MS MQ YT NC NF NU PN PR PF RE SH SJ GS PM SX TC TK UM VG VI WF MP XK".split())

# ---------- countries ----------
countries = {}
for l in lines('countries.txt'):
    iso, cap, lat, lon, pop, area, call, cont = l.split('|')
    countries[iso] = dict(iso=iso, cap=cap, lat=float(lat), lon=float(lon), pop=float(pop), area=float(area), call=call, cont=cont)
rows = []
cur_fix = {'XK': 'EUR', 'BQ': 'USD'}
for iso, c in sorted(countries.items()):
    names = [t.get(iso) or '' for t in (en.territories, ur.territories, ar.territories)]
    if not names[0]:
        raise SystemExit('no name for ' + iso)
    cur = ''
    try:
        lst = get_territory_currencies(iso) if iso != 'XK' else []
        cur = lst[0] if lst else ''
    except Exception:
        cur = ''
    cur = cur or cur_fix.get(iso, '')
    cn = [L.currencies.get(cur, '') if cur else '' for L in (en, ur, ar)]
    sym = en.currency_symbols.get(cur, cur) if cur else ''
    sov = 0 if iso in TERR else 1
    rows.append([iso, alpha3.get(iso, ''), names[0], names[1] or names[0], names[2] or names[0], c['cap'],
                 '%.4f' % c['lat'], '%.4f' % c['lon'], ('%g' % c['pop']), ('%g' % c['area']), c['call'], c['cont'], str(sov),
                 cur, cn[0], cn[1] or cn[0], cn[2] or cn[0], sym])
with open(os.path.join(OUT, 'countries.tsv'), 'w', encoding='utf8') as f:
    f.write('# iso2 iso3 en ur ar capital lat lon popM areaKm2 call continent sov curCode curEn curUr curAr curSym\n')
    for r in rows:
        f.write('\t'.join(r) + '\n')
print('countries', len(rows), 'sovereign', sum(1 for r in rows if r[12] == '1'))

# ---------- extra Urdu/Arabic names ----------
extra = {}
for l in lines('names_x.txt'):
    k, u, a = l.split('|')
    extra[k] = (u, a)

# subdivisions that are really separate territories: use that territory's capital as its point
OVERSEAS = {'FR-BL': 'BL', 'FR-MF': 'MF', 'FR-NC': 'NC', 'FR-PF': 'PF', 'FR-PM': 'PM', 'FR-TF': 'TF', 'FR-WF': 'WF', 'NL-AW': 'AW', 'NL-CW': 'CW', 'NL-SX': 'SX', 'FI-01': 'AX'}

# ---------- subdivisions ----------
subc = {}
for fn in ('subs_1.txt', 'subs_2.txt'):
    for l in lines(fn):
        c, a, b = l.split('|')
        subc[c] = (float(a), float(b))
for s in iso2:
    code = s['code']
    ov = OVERSEAS.get(code)
    if code not in subc and ov in countries:
        subc[code] = (countries[ov]['lat'], countries[ov]['lon'])
srows = []
withc = 0
for s in iso2:
    code = s['code']
    cc = code.split('-')[0]
    la, lo = subc.get(code, (None, None))
    if la is not None:
        withc += 1
    u, a = extra.get(code, ('', ''))
    srows.append([code, s['name'].replace('\t', ' '), s.get('type', ''), s.get('parent', ''), cc,
                  '' if la is None else '%.3f' % la, '' if lo is None else '%.3f' % lo, u, a])
with open(os.path.join(OUT, 'subdivisions.tsv'), 'w', encoding='utf8') as f:
    f.write('# code name type parent country lat lon ur ar\n')
    for r in srows:
        f.write('\t'.join(r) + '\n')
print('subdivisions', len(srows), 'with coordinates', withc, 'first-level', sum(1 for r in srows if not r[3]))
far = []
for r in srows:
    if r[5] and r[4] in countries:
        d = hav(float(r[5]), float(r[6]), countries[r[4]]['lat'], countries[r[4]]['lon'])
        if d > 4500 and r[4] not in ('US', 'RU', 'CA', 'AU', 'CN', 'BR', 'FR', 'NZ', 'GB', 'DK', 'NL'):
            far.append((r[0], r[1], int(d)))
print('far from capital (review):', far)

# ---------- cities ----------
cities = []  # dict
for fn in ('cities_a.txt', 'cities_b.txt', 'cities_c.txt'):
    for l in lines(fn):
        n, iso, la, lo, pop = l.split('|')
        if iso not in countries:
            raise SystemExit('bad iso ' + l)
        cities.append(dict(name=n, iso=iso, lat=float(la), lon=float(lo), pop=int(pop), cap=0))
# capitals
for iso, c in countries.items():
    if c['cap'] in ('-', ''):
        continue
    found = None
    for ct in cities:
        if ct['iso'] == iso and (norm(ct['name']) == norm(c['cap']) or hav(ct['lat'], ct['lon'], c['lat'], c['lon']) < 12 and norm(ct['name'])[:4] == norm(c['cap'])[:4]):
            found = ct
            break
    if found:
        found['cap'] = 1
    else:
        cities.append(dict(name=c['cap'], iso=iso, lat=c['lat'], lon=c['lon'], pop=0, cap=1))
# remove duplicates (same iso + name)
seen = {}
for ct in cities:
    k = (ct['iso'], norm(ct['name']))
    if k in seen:
        seen[k]['cap'] = max(seen[k]['cap'], ct['cap'])
        seen[k]['pop'] = max(seen[k]['pop'], ct['pop'])
    else:
        seen[k] = ct
cities = list(seen.values())
# tz database representative places
def iso6709(s):
    m = re.match(r'([+-])(\d{2})(\d{2})(\d{2})?([+-])(\d{3})(\d{2})(\d{2})?', s)
    sg = 1 if m[1] == '+' else -1
    la = sg * (int(m[2]) + int(m[3]) / 60 + (int(m[4]) if m[4] else 0) / 3600)
    sg = 1 if m[5] == '+' else -1
    lo = sg * (int(m[6]) + int(m[7]) / 60 + (int(m[8]) if m[8] else 0) / 3600)
    return la, lo
tzcity = {}  # (iso, norm name) -> tz
added = 0
for l in open('/usr/share/zoneinfo/zone.tab', encoding='utf8'):
    if l.startswith('#') or not l.strip():
        continue
    cc, co, tz, *_ = l.rstrip('\n').split('\t')
    if cc == 'AQ' or cc not in countries:
        continue
    la, lo = iso6709(co)
    nm = tz.split('/')[-1].replace('_', ' ')
    tzcity[(cc, norm(nm))] = tz
    if any(ct['iso'] == cc and hav(ct['lat'], ct['lon'], la, lo) < 25 for ct in cities):
        continue
    cities.append(dict(name=nm, iso=cc, lat=la, lon=lo, pop=0, cap=0))
    added += 1
print('cities', len(cities), '(added from tz database:', added, ')')
def cldr_city(L, tz):
    try:
        v = L.time_zones.get(tz)
        return (v.get('city') if v else '') or ''
    except Exception:
        return ''
out_c = []
nur = nar = 0
for ct in sorted(cities, key=lambda c: (c['iso'], -c['pop'], c['name'])):
    u = a = ''
    k = 'city:%s:%s' % (ct['name'], ct['iso'])
    if k in extra:
        u, a = extra[k]
    else:
        tz = tzcity.get((ct['iso'], norm(ct['name'])))
        if tz:
            u, a = cldr_city(ur, tz), cldr_city(ar, tz)
    nur += 1 if u else 0
    nar += 1 if a else 0
    out_c.append([ct['name'], ct['iso'], '%.3f' % ct['lat'], '%.3f' % ct['lon'], str(ct['pop']), str(ct['cap']), u, a])
with open(os.path.join(OUT, 'cities.tsv'), 'w', encoding='utf8') as f:
    f.write('# name iso lat lon popThousands isCapital ur ar\n')
    for r in out_c:
        f.write('\t'.join(r) + '\n')
print('cities with Urdu name', nur, 'with Arabic name', nar)
# subdivisions without a hand-made center: use a city of the same name in the same country (if any)
cmap = {}
for ct in cities:
    cmap.setdefault((ct['iso'], norm(ct['name'])), ct)
filled = 0
for r in srows:
    if r[5]:
        continue
    nm = re.sub(r'[\[(].*?[\])]', '', r[1]).strip()
    ct = cmap.get((r[4], norm(nm)))
    if ct:
        r[5], r[6] = '%.3f' % ct['lat'], '%.3f' % ct['lon']
        filled += 1
with open(os.path.join(OUT, 'subdivisions.tsv'), 'w', encoding='utf8') as f:
    f.write('# code name type parent country lat lon ur ar\n')
    for r in srows:
        f.write('\t'.join(r) + '\n')
print('subdivisions placed by same-named city:', filled, '; total with coordinates:', sum(1 for r in srows if r[5]))
# world shapes
subprocess.check_call([sys.executable, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gen_world.py'), os.path.join(OUT, 'world.txt')])
with open(os.path.join(OUT, 'about.txt'), 'w', encoding='utf8') as f:
    f.write('countries=%d\nsubdivisions=%d\ncities=%d\n' % (len(rows), len(srows), len(out_c)))
