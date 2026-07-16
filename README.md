# TaskLeaf

一款原生 Android 任务清单应用，采用 Jetpack Compose + Material 3。交互参考主流任务管理产品，但不包含滴答清单的商标、图标、代码或专有素材。

## 已实现

- 收件箱、今天、已完成三个视图
- 新增、完成、删除任务
- 任务搜索
- 截止日期与四档优先级
- SharedPreferences 本地持久化
- 深色/浅色主题
- GitHub Actions 自动构建 Debug APK

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Android 8.0+（minSdk 26）

## 本地构建

```bash
gradle assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 后续计划

- Room 数据库与数据迁移
- 自定义清单、标签、重复任务
- 日历视图、番茄计时、提醒通知
- WebDAV / 自建服务同步
- 正式签名与 GitHub Release 自动发布
