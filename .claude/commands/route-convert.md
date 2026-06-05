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

**Strip the address suffix** — cut at the first match of any of:
- ` 日本〒`
- `, Kyoto`  `, Tokyo`  `, Osaka`  `, Nara`  `, Hiroshima`
- `, Sakyo Ward`  `, Higashiyama Ward`  `, Yamashina Ward` (any `Ward`)
- `, [digit]` (address number follows)

Take only the text before the cut. That is the place name.

If a segment is bare `lat,lng` with no text — mark as **needs-lookup**.

---

## Coordinate extraction

**Prefer `data=` parameter** — find all `!2m2!1d<lng>!2d<lat>` groups in order.
Note: `!1d` = longitude, `!2d` = latitude in directions URLs.

**Fall back** to parsing path segments as `lat,lng` only if `data=` is absent.

Pair coordinates with names by position (1st segment = 1st coord pair).

---

## Web search — only when path segment is bare coordinates

If the path segment decoded to a real place name → **never search, use it directly.**
Only search when the segment is literally `lat,lng` with no text at all.
Query: `reverse geocode <lat> <lng> place name`

---

## Route metadata

**route_name** — first match wins:
1. User hint (text after the URL)
2. `<first waypoint name> → <last waypoint name>`
3. Timestamp `YYYYMMDDTHHMMSS`

**route_id** — Timestamp `YYYYMMDDTHHMMSS`

**filename** — snake_case of route_name + `.csv`.
Check for collision: run `ls /Users/eva/Documents/projects/gpsanywhere/app/src/main/assets/saved_routes/` and auto-increment suffix (`_1`, `_2`…) if name exists.

---

## CSV format

```
# route_id: <id>
# route_name: <name>
# version: 1
name,latitude,longitude
Place A,35.0054778,135.7736473
Place B,35.0033014,135.7719122
```

- Full coordinate precision from `data=`
- Quote names containing commas: `"My, Place"`

Save with `Write` tool to:
`/Users/eva/Documents/projects/gpsanywhere/app/src/main/assets/saved_routes/<filename>`

---

## Output — compact only

```
✓ saved_routes/<filename>  (<N> waypoints)
 1. <name>   <lat>, <lng>
 2. <name>   <lat>, <lng>
 ...
```

If any names were web-searched, append one line: `⚠ Web-searched: #2, #5`
Nothing else.
