# GPS Anywhere GPS Formatter

Convert Google Maps embed iframe HTML files into route JSON files.

## What it does

- Reads source `.html` files from `input/todo/`
- Extracts iframe `src` and parses Google Maps `pb` data
- Writes JSON files to `output/`
- Moves processed source files to `input/done/`

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

## Run

```bash
go run ./cmd/iframe2json
```

Optional flags:

```bash
go run ./cmd/iframe2json \
  -todo-dir=input/todo \
  -result-dir=output \
  -done-dir=input/done
```

## Useful shell commands

Move all files from `input/done/` back to `input/todo/`:

```bash
mv input/done/* input/todo/
```

Clean all generated JSON files in `output/`:

```bash
rm -f output/*.json
```
