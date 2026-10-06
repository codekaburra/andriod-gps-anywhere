# 首次使用教學：圖片分鏡草案

狀態：首次教學與設定頁重看入口已接入 Android；目前共十頁。

## 畫面原則

- 每頁只畫一支手機，手機外透明；不加旅行風景、手、貼紙或裝飾。
- 所有 App 畫面以手繪手機外框、簡化地圖及灰線內容呈現；操作標籤以圈選或箭頭指向實際控制項。
- 每張 App 畫面同時放繁體中文和英文說明，兩種介面語言共用同一張圖；底部導覽只畫圖示，不畫文字。地點與路線清單可用灰線示意。
- 系統「設定」的第 1–2 頁以 Google Pixel 為視覺參考。其他品牌的路徑可能不同；App 畫面則以目前程式和實際截圖為準。
- 一頁只強調一個動作。下一步按鈕屬於教學介面，不畫進手機內容。

## 建議分鏡

| 頁 | 圖內手機應顯示 | 圖內短標籤（繁中 / English） | 實際操作與依據 |
| --- | --- | --- | --- |
| 1 | Pixel「關於手機」頁**底部**的「版本號碼」列，單一圈選；旁邊寫「按7次 / click 7 times」。圖片下方的教學文字再寫完整路徑「設定 → 關於手機 → 版本號碼」 | 關於手機 / About phone；版本號碼 / Build number；按7次 / click 7 times | **[圖片預覽稿](onboarding/pixel-about-phone-preview.png)**。Google 官方明列 Pixel 路徑為 **Settings → About phone → Build number**；點七次後可能要求輸入螢幕鎖，成功會顯示已成為開發人員。[Android Developers](https://developer.android.com/studio/debug/dev-options.html)、[Android codelab](https://developer.android.com/codelabs/basic-android-kotlin-compose-connect-device)。Pixel 不要畫「軟體資訊」中繼頁。 |
| 2 | Pixel「設定 → 系統 → 開發人員選項」的「選取模擬位置應用程式」列；選擇清單中勾選 GPS Anywhere | 開發人員選項 / Developer options；模擬位置 App / Mock location app；GPS Anywhere | **[圖片已確認保留](onboarding/mock-location-approved.png)**。官方說明啟用後由 Settings → System → Developer options 進入，並在 Debugging 區提供 Select mock location app。[Android codelab](https://developer.android.com/codelabs/basic-android-kotlin-compose-connect-device)、[Android Developers](https://developer.android.com/studio/debug/dev-options.html)。 |
| 3 | GPS Anywhere「設定」頁的「預設資料」卡片，圈出「匯入預設地點」；其他區塊以灰線簡化 | 設定 / Settings；預設資料 / Prebuilt Data；匯入地點 / Import locations | **[共用手繪圖](onboarding/settings-import-final.png)**。依 [設定頁截圖](../docs/screenshots/settings_zh_light.png) 與 [SettingsScreen.kt](../app/src/main/java/com/gpsanywhere/app/ui/settings/SettingsScreen.kt)；預設資料不會在首次開啟時自動匯入。 |
| 4 | GPS Anywhere「位置」頁：圈出下方清單的整張地點卡片，以箭頭指出點選位置 | 點選地點 / Tap a location | **[共用手繪圖](onboarding/location-list-final.png)**。依 [位置頁截圖](../docs/screenshots/location_zh_light.png) 與 [LocationScreen.kt](../app/src/main/java/com/gpsanywhere/app/ui/location/LocationScreen.kt)。 |
| 5 | GPS Anywhere「位置」頁：上方座標欄、貼上按鈕、方向鍵，以及由左至右的門、步行、極速移動三個按鈕；三種移動說明放在各自按鈕旁並以短箭頭指出 | 輸入座標 / Enter coordinates；貼上座標 / Paste coordinates；方向鍵 / D-pad；跳至 / Jump；繞圈步行 / Walk Around；極速移動到目標 / Move to target at top speed | **[共用手繪圖](onboarding/custom-coordinates-final.png)**。貼上按鈕讀取剪貼簿座標；方向鍵於定位啟動後微調；Jump 立即到達，Walk Around 繞圈移動，極速移動以 5000 km/h 飛往目標。 |
| 6 | 「路線」頁的速度面板：箭頭指向滑桿把手，圈出速度讀數；下方路線保持灰線 | 拖曳調整速度 / Drag to change speed；速度 / Speed | **[共用手繪圖](onboarding/speed-panel-final.png)**。拖曳速度滑桿可調整路線移動速度，行走中也可調整。 |
| 7 | 「路線」頁：地圖右上角的＋新增路線；下方已儲存路線只圈出名稱區域，避開編輯和刪除按鈕 | 新增路線 / Add route；點選路線 / Tap a route | **[共用手繪圖](onboarding/route-view-final.png)**。依 [Route 畫面](../docs/screenshots/route_en_light.png) 與路線頁操作。 |
| 8 | 「新增地點」表單：中英文名稱欄、緯度與經度、選填標籤、儲存按鈕 | 填一種 / Either name；經緯度 / Lat, long；可選 / Optional | **[共用手繪圖](onboarding/add-location-final.png)**。名稱擇一、經緯度必填，標籤選填。 |
| 9 | 「新增／編輯路線」頁：路線名稱、點地圖加入途經地點、輸入座標後按＋、調整途經地點次序、儲存 | 命名路線 / Name the route；點擊地圖新增途經地點 / Tap map to add a waypoint；座標＋ / Coordinate +；次序調整 / Reorder；至少兩站 / 2+ stops | **[共用手繪圖](onboarding/route-editor-final.png)**。至少兩個途經地點才能儲存。 |
| 10 | 路線編輯的 CSV 模式：路線名稱、CSV 範例欄、貼上、複製、套用；頂部 Map 可返回地圖，套用後才可儲存 | 按 CSV 匯入 / Tap CSV to import；貼上路線 CSV / Paste route CSV；套用後按儲存 / Apply, then Save | **[共用手繪圖](onboarding/route-csv-final.png)**。每筆資料列為 `latitude,longitude,name_tc,name_en`；按套用後回到地圖編輯模式，再按儲存。 |
