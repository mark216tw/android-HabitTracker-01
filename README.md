# 習慣追蹤單機版

以 Kotlin 與 Jetpack Compose 開發的 Android 飲水與習慣追蹤 App。所有使用者資料都儲存在裝置本機，不需要註冊帳號或連線到後端服務。

## 主要功能

- 250 ml、500 ml、600 ml 一鍵飲水記錄
- 動態水杯與每日飲水目標進度
- 飲水誤觸後可透過 Snackbar 復原上一筆
- 習慣新增、編輯及每日快速打卡
- 習慣封存、歷史查看、恢復與永久刪除
- 每天、工作日或自選星期的習慣排程
- 單一習慣月曆打卡牆與最近七天補登
- 最近七天、目前連續、最佳連續與本月完成統計
- 每兩小時飲水提醒及個別習慣提醒
- 通知權限與頻道狀態提示
- 系統、淺色、深色顯示模式
- 六組預設主題色與自訂 Hue 滑桿
- 原創記事本勾選圖示，支援 Android adaptive、round 與 Android 13 themed icon

## 技術規格

- Kotlin 2.1.20
- Jetpack Compose + Material 3
- Room
- DataStore Preferences
- WorkManager
- Navigation Compose
- Coroutines + Flow
- Min SDK 26
- Target SDK 35

## 開始建置

需求：JDK 17 與 Android SDK 35。

```powershell
.\gradlew.bat assembleDebug
```

產生 R8 壓縮、資源縮減並使用 Debug 金鑰簽署的測試發行 APK：

```powershell
.\gradlew.bat testDebugUnitTest testPrereleaseUnitTest compileDebugAndroidTestKotlin lintPrerelease assemblePrerelease
```

APK 輸出位置：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 版本資訊

- Build Type：`prerelease`
- Version Code：`1`
- Version Name：`1.0.0-prerelease`
- Application ID：`com.example.habitwater`
- 簽署：Android Debug certificate，僅供測試發行

## 文件

- [文件索引](docs/README.md)
- [產品與功能規格](docs/PRODUCT.md)
- [技術架構](docs/ARCHITECTURE.md)
- [建置與測試發行](docs/BUILD_AND_RELEASE.md)

## 注意事項

- WorkManager 提醒會受到 Android 省電與背景執行政策影響，不保證精準到分鐘。
- Android 13 以上需要使用者允許通知權限。
- `prerelease` APK 使用 Debug 金鑰，不可作為正式商店上架版本。

## 授權

本專案採用 [MIT License](LICENSE)。
