# Play Store graphic assets

| File | Spec | Play requirement |
|---|---|---|
| `icon-512.png` | 512 × 512 PNG, 204 KB | ≤ 1 MB, 512 × 512 |
| `feature-graphic-en.png` | 1024 × 500 PNG, 340 KB | ≤ 15 MB, 1024 × 500 |
| `feature-graphic-zh.png` | 1024 × 500 PNG, 339 KB | same, for the 繁體中文 listing |

The icon is a straight copy of `app/src/main/res/drawable-nodpi/ic_launcher_bg.png`,
which is what the adaptive icon already draws — so the store icon and the launcher
icon are the same artwork. Play applies its own corner mask, so it is left
full-bleed square.

The feature graphics are rendered from the HTML in `src/`. To regenerate after an
edit:

```bash
cd docs/play-store/src && for l in en zh; do "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size=1024,500 --screenshot="../feature-graphic-$l.png" --allow-file-access-from-files "file://$PWD/feature-$l.html"; done
```

Note that headless Chrome has no SF Pro, so the type falls through to Helvetica
Neue; keep `"PingFang TC"` *after* Helvetica in the stack of `feature-zh.html`,
or the Latin title picks up PingFang's lighter Latin glyphs.

Layout keeps all text and the icon at least 70 px from the edges, since Play crops
the feature graphic differently across placements and may overlay a play button on
it when a promo video is set.
