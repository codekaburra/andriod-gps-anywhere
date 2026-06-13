# /route-convert

Convert a Google Maps directions URL → GPS Anywhere route CSV and save it.
**Output only the result — no step-by-step narration.**

---

## Input format

```
<URL>
[optional route name hint]
```

Multiple URLs separated by blank lines = batch mode (convert and save each).

---

## Name extraction — decode path segments

Split between `/maps/dir/` and `/@`. Each `/`-separated segment is one waypoint.

Decode: replace `+` → space, decode `%XX` sequences.

**Strip the address suffix** — cut at the first ` 香港` or at any of:
- ` 日本〒`
- `, Kyoto`  `, Tokyo`  `, Osaka`  `, Nara`  `, Hiroshima`
- `, Sakyo Ward`  `, Higashiyama Ward`  `, Yamashina Ward` (any `Ward`)
- `, [digit]` (address number follows)

Take only the text before the cut. That is the place name (Name_TC).

If a segment is bare `lat,lng` with no text — use empty string for Name_TC and Name_EN. **Do NOT web search.**

**Name_EN** — translate the Name_TC to English. For well-known landmarks use the official English name. For bare coordinates, leave empty.

---

## Coordinate extraction

**Prefer `data=` parameter** — find all `!2m2!1d<lng>!2d<lat>` groups in order.
Note: `!1d` = longitude, `!2d` = latitude in directions URLs.

**Fall back** to parsing path segments as `lat,lng` only if `data=` is absent.

For segments with `!1m0!` in data (bare coordinate waypoints with no Place ID), use the coordinates from the path segment directly.

Pair coordinates with names by position (1st segment = 1st coord pair).

---

## Route metadata

**route_name** — first match wins:
1. User hint (text after the URL)
2. `<first waypoint name> → <last waypoint name>`
3. Timestamp `YYYYMMDDTHHMMSS`

**route_id** — Timestamp `YYYYMMDDTHHMMSS`

**filename** — user-specified filename if given, otherwise snake_case of route_name + `.csv`.
Check for collision: run `ls /Users/eva/Documents/projects/gpsanywhere/app/src/main/assets/saved_routes/` and auto-increment suffix (`_1`, `_2`…) if name exists.

---

## CSV format

```
# version: 1
# route_name: <name>
latitude,longitude,name_tc,name_en
22.2799747,114.1893765,香港中央圖書館,HK Central Library
35.0054778,135.7736473,祇園白川,Gion Shirakawa
```

- Full coordinate precision from `data=`
- Quote fields containing commas: `"My, Place"`

Save with `Write` tool to:
`/Users/eva/Documents/projects/gpsanywhere/app/src/main/assets/saved_routes/<filename>`

---

## Output — compact only

```
✓ saved_routes/<filename>  (<N> waypoints)
 1. <Name_TC> (<Name_EN>)   <lat>, <lng>
 2. <Name_TC> (<Name_EN>)   <lat>, <lng>
 ...
```

Nothing else.
