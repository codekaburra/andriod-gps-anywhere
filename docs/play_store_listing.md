# Play Store listing — draft

Positioned as a developer and QA tool throughout. Google Play allows mock-location
apps as testing utilities, but rejects listings that pitch them as a way to
misrepresent your location to other services — so nothing here promises that,
and the disclaimer appears in the full description.

Field limits: app name 30 characters, short description 80, full description 4000.

---

## English

### App name (30)

```
GPS Anywhere
```

### Short description (80)

```
Mock your device location for app testing — jump, walk routes, control speed.
```

*77 characters.*

### Full description (4000)

```
GPS Anywhere is a mock-location tool for developers and QA testers who need to
put an Android device somewhere else — and keep it moving realistically — while
they work on location-aware features.

It uses Android's own test-provider API, so it only works once you have enabled
Developer Options and selected GPS Anywhere as the mock location app. Nothing is
hidden or automatic; you turn it on deliberately.

WHAT IT DOES

• Jump — place the device at any coordinate and hold it there.
• Walk Around — wander in a spiral around a point, so the device looks occupied
  rather than frozen at one pixel.
• Max-Speed Travel — travel from where you are to a target at high speed, then
  settle into a wander at the far end.
• Routes — walk a series of waypoints. Pause, resume, reverse direction, or tap
  a waypoint to jump ahead. Build routes by tapping the map, pasting
  coordinates, or editing them as CSV.
• Speed control — a non-linear slider that gives walking speeds most of its
  travel, because that is the range worth fine-tuning. Real speed drifts around
  the value you set, so tracks do not look machine-generated.
• Direction pad — nudge the position by small steps without retyping anything.

SAVED PLACES

Save any coordinate with a name and tags, in English and Chinese, and the list
follows whichever language the app is set to. Sample locations and routes are
bundled but never imported automatically — you choose, from Settings, and you
can delete them again.

BUILT FOR THE JOB

• Runs as a foreground service with a persistent notification, so a long walk
  survives the screen going off.
• OpenStreetMap tiles — no Google Play Services dependency.
• Light and dark themes, both hand-tuned rather than one inverted.
• English and 繁體中文 throughout, including saved data.
• No account, no sign-in, no analytics. Your places stay on your device.

REQUIREMENTS

Android 8.0 or newer, and Developer Options enabled. The app includes a setup
guide and a shortcut into the relevant system screen.

IMPORTANT

This app is intended for development and testing purposes only. Simulating your
location may breach the terms of service of other apps and services, and may be
restricted by law where you live. You are responsible for how you use it. Do not
use it to misrepresent your location to services that rely on it.
```

---

## 繁體中文

### 應用程式名稱 (30)

```
GPS Anywhere
```

### 簡短說明 (80)

```
為 App 測試模擬裝置定位：瞬移、走路線、調整速度。
```

### 完整說明 (4000)

```
GPS Anywhere 是給開發者與測試人員使用的模擬定位工具。開發需要位置的功能時，可
以把裝置放到任何座標，並讓它以合理的方式持續移動。

它使用 Android 內建的 test provider 機制，因此必須先啟用「開發人員選項」並把
GPS Anywhere 設為模擬位置應用程式才會生效。沒有任何隱藏或自動啟動的行為，一切
由你主動開啟。

功能

• 瞬移 — 直接把裝置定位到指定座標，停在那裡不動。
• 繞圈步行 — 在該點周圍繞圈走，讓裝置看起來是有人在使用，而不是釘死在一個點。
• 極速移動 — 從目前位置以高速前往目標，抵達後自動接上繞圈步行。
• 路線 — 沿著一連串途經點行走，可暫停、繼續、反向，或點某個途經點直接跳過去。
  建立路線可以點地圖、貼上座標，或直接編輯 CSV。
• 速度調整器 — 非線性滑桿，走路速度佔了大部分行程，因為那才是需要細調的範圍。
  實際速度會在設定值附近浮動，軌跡不會像機器產生的。
• 方向鍵 — 小幅度微調位置，不必重新輸入座標。

儲存地點

每個座標都可以存成有名稱和標籤的地點，中英文各一組，清單會跟著 App 的語言顯示。
內建範例地點與路線，但不會自動匯入 — 由你在「設定」裡決定，也可以隨時刪除。

其他

• 以前景服務執行並顯示常駐通知，長時間行走不會因為螢幕關閉而中斷。
• 使用 OpenStreetMap 圖磚，不依賴 Google Play 服務。
• 淺色與深色主題各自調校，不是把同一組顏色反轉。
• 介面與資料皆支援 English 與繁體中文。
• 不需註冊、不需登入、不做分析追蹤，你的地點只留在裝置上。

系統需求

Android 8.0 以上，並需啟用開發人員選項。App 內附設定教學與系統設定捷徑。

重要聲明

本 App 僅供開發與測試用途。模擬定位可能違反其他 App 或服務的服務條款，在部分
地區也可能受法律限制。使用方式與後果由你自行負責。請勿用於對依賴定位的服務謊報
所在位置。
```

---

## Other Play Console fields

| Field | Suggested answer |
|---|---|
| Category | Tools |
| Tags | Developer tools, Testing |
| Content rating | Everyone — no user-generated content, no ads, no purchases |
| Ads | No |
| In-app purchases | No |
| Data safety — collected | None. Saved places and routes stay in a local Room database; nothing is uploaded. |
| Data safety — shared | None |
| Data safety — location | Accessed to centre the map on the real position; not collected or transmitted |
| Target audience | 18+ (a developer tool, not aimed at children) |
| Privacy policy | Required — a URL must be hosted before the listing can be submitted |

## Still needed before submission

- **512 × 512 app icon** (PNG, 32-bit)
- **1024 × 500 feature graphic**
- **Phone screenshots** — the 20 in `docs/screenshots/` are 1080 × 2424, within Play's limits; pick 2–8
- **Privacy policy URL** — must state that location is read locally and nothing is collected
