# 建置與測試發行

## 環境需求

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.1 或相容版本
- Windows、macOS 或 Linux

專案包含 Gradle Wrapper，不需要另外安裝全域 Gradle。

## 常用命令

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat testPrereleaseUnitTest
.\gradlew.bat compileDebugAndroidTestKotlin
.\gradlew.bat lintPrerelease
.\gradlew.bat assemblePrerelease
```

macOS 或 Linux：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew testPrereleaseUnitTest
./gradlew compileDebugAndroidTestKotlin
./gradlew lintPrerelease
./gradlew assemblePrerelease
```

完整驗證：

```powershell
.\gradlew.bat testDebugUnitTest testPrereleaseUnitTest compileDebugAndroidTestKotlin lintPrerelease assemblePrerelease
```

## Prerelease Build Type

`prerelease` 設定位於 `app/build.gradle.kts`：

- `isMinifyEnabled = true`
- `isShrinkResources = true`
- 使用 `proguard-android-optimize.txt`
- 使用 Debug signing configuration
- Version Name：`1.0.0-prerelease`
- Version Code：`1`

輸出檔案：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## APK 驗證

檢查套件名稱、版本與 SDK：

```powershell
& "$env:ANDROID_HOME\build-tools\35.0.1\aapt.exe" dump badging `
  "app\build\outputs\apk\prerelease\app-prerelease.apk"
```

檢查 APK 簽章：

```powershell
& "$env:ANDROID_HOME\build-tools\35.0.1\apksigner.bat" verify --verbose --print-certs `
  "app\build\outputs\apk\prerelease\app-prerelease.apk"
```

目前 prerelease APK 預期資訊：

| 欄位 | 值 |
| --- | --- |
| Application label | 習慣追蹤單機版 |
| Application ID | `com.example.habitwater` |
| Version Name | `1.0.0-prerelease` |
| Version Code | `1` |
| Min SDK | `26` |
| Target SDK | `35` |
| Signing certificate | Android Debug |

## 測試

目前 JVM 單元測試涵蓋星期 bit mask、日期區間、提醒排程、最近七天、本月與 streak 統計。Android instrumentation 測試原始碼涵蓋 Room 首次資料、封存／恢復、永久刪除、completion 防護與 v1 → v2 migration；`compileDebugAndroidTestKotlin` 可在沒有裝置時驗證測試程式，連接實機或模擬器後可執行：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Lint、R8、資源縮減、adaptive icon 資源與 APK packaging 會在 prerelease 建置流程中驗證。

正式發布前仍應補充：

- 實機或模擬器 Compose UI 測試
- Android 13+ 通知權限流程
- Doze 模式及廠商省電限制測試
- 跨午夜、時區變更及 Room migration 的實機測試
- 正式 signing key 與 Play App Signing 設定

## 正式發行注意事項

Debug 金鑰只適合內部測試。正式上架前必須建立並妥善保管 release keystore，將密碼放在未提交的環境變數或本機設定中，不能提交 keystore、密碼或憑證到 Git。
