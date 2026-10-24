#!/usr/bin/env python3
"""Converts a Natural Earth countries GeoJSON into app/src/main/assets/world.txt (with country borders).

1. On a computer with internet download "ne_110m_admin_0_countries.geojson" (Natural Earth, public domain),
   for example from https://www.naturalearthdata.com/ (Admin 0 - Countries, 1:110m) or the "geojson" mirror of it.
2. Run:  python3 tools/make_world.py ne_110m_admin_0_countries.geojson app/src/main/assets/world.txt [tolerance_degrees]
3. Rebuild the app. The map now draws country borders, highlights the tapped country and can name the
   country under a long press.
Use the 110m file (about 10 000 points). Larger files make the map slow.
Output format, one polygon per line:  L <ISO2 or -> lon lat lon lat ...   (W = water polygon)
"""
import json, sys

def rdp(pts, eps):
    if len(pts) < 3 or eps <= 0:
        return pts
    (x1, y1), (x2, y2) = pts[0], pts[-1]
    dx, dy = x2 - x1, y2 - y1
    norm = (dx * dx + dy * dy) ** 0.5
    best, idx = -1.0, 0
    for i in range(1, len(pts) - 1):
        x, y = pts[i]
        d = abs(dy * x - dx * y + x2 * y1 - y2 * x1) / norm if norm else ((x - x1) ** 2 + (y - y1) ** 2) ** 0.5
        if d > best:
            best, idx = d, i
    if best > eps:
        return rdp(pts[:idx + 1], eps)[:-1] + rdp(pts[idx:], eps)
    return [pts[0], pts[-1]]

def iso_of(props):
    for k in ('ISO_A2_EH', 'ISO_A2', 'iso_a2', 'WB_A2', 'POSTAL'):
        v = props.get(k)
        if isinstance(v, str) and len(v) == 2 and v.isalpha():
            return v.upper()
    if props.get('NAME') == 'France' or props.get('name') == 'France':
        return 'FR'
    if props.get('NAME') == 'Norway' or props.get('name') == 'Norway':
        return 'NO'
    return '-'

def main():
    src, out = sys.argv[1], sys.argv[2]
    eps = float(sys.argv[3]) if len(sys.argv) > 3 else 0.0
    gj = json.load(open(src, encoding='utf8'))
    lines = []
    for f in gj['features']:
        iso = iso_of(f.get('properties', {}))
        g = f['geometry']
        if g is None:
            continue
        polys = g['coordinates'] if g['type'] == 'MultiPolygon' else [g['coordinates']]
        for poly in polys:
            ring = [(float(p[0]), float(p[1])) for p in poly[0]]  # outer ring only (holes are skipped)
            ring = rdp(ring, eps)
            if len(ring) < 4:
                continue
            if ring[0] == ring[-1]:
                ring = ring[:-1]
            lines.append('L %s %s' % (iso, ' '.join('%g %g' % p for p in ring)))
    open(out, 'w').write('\n'.join(lines) + '\n')
    print(len(lines), 'polygons written to', out)

if __name__ == '__main__':
    main()
