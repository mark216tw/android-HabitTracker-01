# 技術架構

## 整體設計

專案採單一 Activity 與 Jetpack Compose UI，透過 `MainViewModel` 整合 Room、DataStore 與 WorkManager。畫面使用 `StateFlow` 觀察狀態並自動重新組合。

```text
Compose screens
      |
MainViewModel
      |
      +-- AppRepository ------ Room database
      +-- SettingsRepository - DataStore Preferences
      +-- ReminderScheduler -- WorkManager
```

## 原始碼結構

```text
app/src/main/java/com/example/habitwater/
|-- MainActivity.kt
|-- HabitWaterApplication.kt
|-- data/
|   |-- AppDatabase.kt
|   |-- AppRepository.kt
|   |-- AppSettings.kt
|   |-- Daos.kt
|   |-- HabitStats.kt
|   `-- Models.kt
|-- notification/
|   `-- Reminders.kt
`-- ui/
    |-- MainViewModel.kt
    |-- ArchivedHabitsScreen.kt
    |-- HomeScreen.kt
    |-- CalendarScreen.kt
    |-- SettingsScreen.kt
    |-- CommonUi.kt
    `-- theme/AppTheme.kt
```

## 本機資料

### Room

`Habit`

- `id`：自動產生的主鍵
- `name`：習慣名稱
- `daysMask`：週一至週日的 bit mask
- `reminderHour`、`reminderMinute`：選用提醒時間
- `createdAt`：建立時間
- `archivedOn`：封存的本地日期；`NULL` 代表使用中
- `streakResetOn`：恢復習慣後 current streak 的重新計算起點

`HabitArchivePeriod`

- 保存每次封存與恢復的本地日期
- 用於依目前排程重算歷史最佳 streak，同時避免跨恢復邊界串接

`HabitCompletion`

- 複合主鍵為 `habitId` 與 ISO-8601 本地日期
- 與 `Habit` 建立外鍵，刪除習慣時採 cascade delete

`WaterEntry`

- `id`：自動產生的主鍵
- `amountMl`：飲水容量
- `recordedAt`：記錄時間戳

習慣完成狀態使用本地日期，避免日曆查詢受到時分秒影響；飲水紀錄使用時間戳，依系統時區換算每日區間。

`MainViewModel` 維護目前本地日期與時區。App 回到前景或在前景跨日後會刷新狀態，並以 `flatMapLatest` 切換當日飲水查詢，讓首頁、月曆與打卡共用同一日期來源。

預設習慣由 Room database `onCreate` callback 寫入，只在資料庫第一次建立時執行。

資料庫 version 2 透過 `MIGRATION_1_2` 增加封存、streak 邊界欄位與封存區間表。Room schema 匯出至 `app/schemas` 並納入版本控制，migration 不使用 destructive fallback。

一般習慣查詢只回傳 `archivedOn IS NULL` 的資料；封存清單使用獨立查詢。封存不刪除 completion，永久刪除則只允許作用於已封存資料並沿用外鍵 cascade。完成狀態 transaction 會再次確認習慣仍為 active。

`HabitStats` 由純 Kotlin 計算單一習慣的最近七天、本月、目前 streak 與最佳 streak。完成次數包含非排定日補登，streak 只使用目前 `daysMask` 的排定日。封存統計以 `archivedOn` 為基準，恢復後 current streak 不跨越 `streakResetOn`。

### DataStore

DataStore 保存：

- 顯示模式
- 主題 Hue
- 每日飲水目標
- 飲水提醒開關
- 提醒開始與結束小時

## 通知架構

`WaterReminderWorker` 每兩小時執行，讀取最新設定並確認目前時間位於允許時段後才通知。

每個有提醒時間的習慣建立一個名稱唯一的單次 `HabitReminderWorker`。Worker 執行時會讀取最新習慣與完成資料，確認今天是否排定且尚未完成，再依最新本地日期、時區與星期設定安排下一次工作。

WorkManager 自動持久化工作並處理裝置重新啟動。App 回到前景時會依 Room 與 DataStore 重新核對排程。Android 13 以上於使用者首次啟用提醒時要求 `POST_NOTIFICATIONS` 權限；拒絕權限不會清除提醒設定，設定頁會顯示投遞狀態並提供系統設定入口。

## 主題架構

主題由顯示模式與 Hue 建立 Material 3 light/dark color scheme。Hue 使用 HSL 轉色，secondary 色彩由主 Hue 位移產生。

系統列採 edge-to-edge 顯示，Compose 主題改變時同步更新狀態列與導覽列圖示明暗。

## App 圖示架構

- `drawable/ic_launcher_foreground.xml`：108 × 108 dp 彩色記事本與勾選前景。
- `drawable/ic_launcher_monochrome.xml`：Android 13 themed icon 單色輪廓。
- `drawable/ic_launcher_legacy.xml`：舊版 Android 的背景與前景組合。
- `mipmap-anydpi-v26`：adaptive 與 round icon。
- `mipmap-anydpi-v33`：加入 monochrome layer 的 adaptive 與 round icon。

核心圖案連同粗線條控制在約 59.4 dp，置於 66 × 66 dp 安全區內；背景使用獨立純色 layer，讓 Launcher 可套用圓形、圓角方形及其他遮罩。

## 導覽

- `home`：飲水進度與今日習慣
- `calendar`：單一習慣月曆
- `settings`：目標、提醒及主題設定
- `archived`：已封存習慣清單
- `archived/{habitId}`：封存習慣的唯讀月曆、統計與管理

首頁與月曆使用底部導覽列。設定頁從右上角齒輪進入，系統返回鍵與畫面返回按鈕都會回到上一個畫面。
