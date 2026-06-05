# GPS Anywhere GPS Formatter

Convert between Google Maps embed iframe HTML and GPS Anywhere route JSON.

## Commands

### `iframe2json`

- Reads source `.html` files from `cmd/iframe2json/input/`
- Extracts iframe `src` and parses Google Maps `pb` data
- Writes JSON files to `cmd/iframe2json/output/`
- Keeps source HTML files in place by default
- With `--once`, processes one file only and moves it to `cmd/iframe2json/input/done/`

### `json2iframe`

- Reads source `.json` files from `cmd/json2iframe/input/`
- Generates iframe HTML files into `cmd/json2iframe/output/`
- Keeps source JSON files in place by default
- With `--once`, processes one file only and moves it to `cmd/json2iframe/input/done/`

## Input file format

Use one `.html` file per route. Each file should contain a Google Maps iframe, for example:

```html
<iframe src="https://www.google.com/maps/embed?pb=..."></iframe>
```

Recommended source filename:

- `tainan_city_1.html`
- `taipei_city_1.html`

## Output filename rules

- Output filename format is `<route_name>_<timestamp>.json`
- Timestamp format is `YYYYMMDDHHmmSSS` (milliseconds, no seconds)
- For `tainan_city_1.html`, output looks like `tainan_city_1_202606051523000.json`

## JSON shape

```json
{
  "route_id": "202606051523000",
  "route_name": "tainan_city_1",
  "version": 1,
  "coordinates": [
    {
      "name": "Place Name",
      "latitude": 22.0,
      "longitude": 120.0
    }
  ]
}
```

## Run `iframe2json`

```bash
go run ./cmd/iframe2json
```

Default behavior (when no flags are passed):

- `-input-dir=cmd/iframe2json/input`
- `-output-dir=cmd/iframe2json/output`
- `done dir is auto-derived as <input-dir>/done`
- `-once=false` (process all `.html` files and do not move source files)

Optional flags:

```bash
go run ./cmd/iframe2json \
  -input-dir=cmd/iframe2json/input \
  -output-dir=cmd/iframe2json/output \
  -once
```

## Run `json2iframe`

```bash
go run ./cmd/json2iframe
```

Default behavior (when no flags are passed):

- `-input-dir=cmd/json2iframe/input`
- `-output-dir=cmd/json2iframe/output`
- `done dir is auto-derived as <input-dir>/done`
- `-once=false` (process all `.json` files and do not move source files)
- `-width=600`
- `-height=450`
- `-loading=lazy`
- `-referrerpolicy=no-referrer-when-downgrade`

Optional flags:

Convert current Android app built-in routes to iframe output:

```bash
go run ./cmd/json2iframe --from-app-routes
```

Equivalent explicit command:

```bash
go run ./cmd/json2iframe \
  -input-dir=../app/src/main/assets/saved_routes \
  -output-dir=cmd/json2iframe/output \
  -once \
  -width=600 \
  -height=450 \
  -loading=lazy \
  -referrerpolicy=no-referrer-when-downgrade
```



## Useful shell commands (`iframe2json`)

Move all files from `cmd/iframe2json/input/done/` back to `cmd/iframe2json/input/`:

```bash
mv cmd/iframe2json/input/done/* cmd/iframe2json/input/
```

Clean all generated JSON files in `cmd/iframe2json/output/`:

```bash
rm -f cmd/iframe2json/output/*.json
```

