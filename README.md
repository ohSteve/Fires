# 整点薯条

<p align="center"><img src="docs/app-logo.png" width="160" alt="整点薯条 Logo"></p>

完全离线的个人目标 / 打卡 / 待办 / 日志管理 Android 应用。没有网络权限，所有数据只保存在本机。

## 功能

- **目标**：DDL 设定、里程碑时间轴、实现目标后锁定编辑
- **打卡**：每日打卡、按周期（每天 / 每周指定日）规划、补卡、打卡项目管理与暂停
- **待办**：DDL 颜色提醒（当天橙色 / 过期红色）、完成记录按月份筛选、列表折叠
- **日志**：每天自动汇总实现的目标、完成的里程碑、打卡与待办，另可填写手动笔记、使用时长、娱乐项目
- **设置**：6 套主题预设 + 自定义主色、每日提醒、数据导出 / 导入

## 版本

| 标签 | 应用版本 | 日期 | 说明 |
|------|----------|------|------|
| [`v1`](../../releases/tag/v1) | 1.0（versionCode 1） | 2026-07-09 | 初始完成版本（`-final`） |
| [`v2`](../../releases/tag/v2) | 1.6（versionCode 7） | 2026-10-17 | 当前版本：全局检索、日志数据完整性修复、新增后不可见修复等 |

当前 `main` 分支为 v2（1.6）。查看历史版本：`git checkout v1` 或 `git checkout v2`。每个标签的 `apk/` 目录内有对应的可安装 APK。

## 技术栈

Kotlin 1.9.24 · Jetpack Compose（BOM 2024.05.00）· Material 3 · Room 2.6.1 · KSP 1.9.24-1.0.20 · MVVM · Gradle 8.13 / AGP 8.13.2 · minSdk 26 / targetSdk 34

架构刻意保持简单：单 Activity + 状态驱动导航（`sealed class Screen`）、手动数据库单例，不使用 Hilt / Navigation Compose / 网络依赖。

## 构建

环境要求：JDK 17、Android SDK（API 34）。

```bash
# 1. 在根目录创建 local.properties，写入 SDK 路径
# sdk.dir=D\:\\sdk

# 2. 构建 Debug APK
./gradlew assembleDebug

# 输出：app/build/outputs/apk/debug/app-debug.apk
```

更完整的技术说明见 [项目技术文档](docs/项目技术文档.md)。

## 说明

- 应用完全离线，不申请 `INTERNET` 权限。
- `local.properties`、构建产物、IDE 配置等未纳入版本控制。
