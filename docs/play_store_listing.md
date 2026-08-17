# Play Store listing — draft

Short, testing-focused copy for Google Play. Paste only the text inside each
code block into Play Console.

Field limits: app name 30 characters, short description 80, full description 4000.

---

## English

### App name (30)

```
GPS Anywhere
```

### Short description (80)

```
Mock location tools for Android developers and QA testers.
```

*56 characters.*

### Full description (4000)

```
GPS Anywhere helps developers and QA testers test location-aware Android apps.

• Set a test coordinate.
• Simulate movement around a point or along a route.
• Create routes from map taps, pasted coordinates, or CSV.
• Pause, reverse, adjust speed, and nudge the current test position.
• Save locations and routes on your device.

SETUP

Enable Developer Options, then select GPS Anywhere as the mock location app.
Android 8.0 or newer is required. The app includes a setup guide.

No account is required. English and Traditional Chinese are supported.

For development and testing only. Do not use this app to misrepresent your
location to services that rely on it.
```

---

## 繁體中文

### 應用程式名稱 (30)

```
GPS Anywhere
```

### 簡短說明 (80)

```
供 Android 開發與 QA 測試使用的模擬定位工具。
```

### 完整說明 (4000)

```
GPS Anywhere 協助開發者與測試人員測試需要定位功能的 Android App。

• 設定測試座標。
• 模擬定點周圍或路線上的移動。
• 以地圖點選、貼上座標或 CSV 建立路線。
• 暫停、反向、調整速度，或微調目前的測試位置。
• 在裝置上儲存地點與路線。

設定方式

啟用「開發人員選項」後，將 GPS Anywhere 選為模擬位置應用程式。
需要 Android 8.0 以上；App 內附設定教學。

不需帳號。支援 English 與繁體中文。

本 App 僅供開發與測試用途。請勿用於對依賴定位的服務謊報所在位置。
```

---

## Other Play Console fields

| Field | Suggested answer |
|---|---|
| Category | Tools |
| Tags | Developer tools, Testing |
| Content rating | Everyone — no user-generated content or purchases |
| Ads | Yes — banner and interstitial ads are shown through Google Mobile Ads SDK. |
| In-app purchases | No |
| Data safety — collected | Complete this from the Google Mobile Ads SDK's current Play Console guidance. Saved places and routes remain in a local Room database. |
| Data safety — shared | Complete this from the Google Mobile Ads SDK's current Play Console guidance. |
| Data safety — location | Used to centre the map on the device's position. Declare any SDK data handling accurately in Play Console. |
| Target audience | 18+ (a developer tool, not aimed at children) |
| Privacy policy | Required — a URL must be hosted before the listing can be submitted |

## Still needed before submission

- **512 × 512 app icon** (PNG, 32-bit)
- **1024 × 500 feature graphic**
- **Phone screenshots** — the 20 in `docs/screenshots/` are 1080 × 2424, within Play's limits; pick 2–8
- **Privacy policy URL** — must cover local location handling and the advertising SDK's data practices
