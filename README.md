# 🕷️ Web Scraper — 纯 Android 网页采集器

> 一个纯 Android 本地运行的网页数据采集工具，无需后端服务器。
> 支持视觉框选元素、CSS/XPath 规则、JS 动态渲染、定时采集、JSON/Excel 导出。

---

## 📐 系统架构

```
┌─────────────────────────────────────────────────┐
│                 Android App (纯本地)              │
│                                                  │
│  ┌─────────────────────────────────────────────┐ │
│  │  UI Layer (Jetpack Compose)                  │ │
│  │  底部导航: 任务 / 统计 / 设置                  │ │
│  │  核心页面: 视觉框选 WebView                    │ │
│  └──────────────┬──────────────────────────────┘ │
│                 │                                 │
│  ┌──────────────┴──────────────────────────────┐ │
│  │  Core Engine                                 │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐ │ │
│  │  │ OkHttp   │ │ WebView  │ │ Jsoup Parser │ │ │
│  │  │ 请求     │ │ JS渲染   │ │ CSS/XPath    │ │ │
│  │  └──────────┘ └──────────┘ └──────────────┘ │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐ │ │
│  │  │ 反爬策略 │ │ WorkMgr  │ │ Excel/JSON   │ │ │
│  │  │ UA/代理  │ │ 定时调度 │ │ 导出         │ │ │
│  │  └──────────┘ └──────────┘ └──────────────┘ │ │
│  └──────────────┬──────────────────────────────┘ │
│                 │                                 │
│  ┌──────────────┴──────────────────────────────┐ │
│  │  Room Database (SQLite)                      │ │
│  └─────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

## 🚀 功能特性

| 功能 | 描述 |
|------|------|
| ✅ 视觉框选 | 在 WebView 中点击元素，自动识别同类元素并生成 CSS 规则 |
| ✅ CSS/XPath 规则 | 支持手写规则或自动生成 |
| ✅ JS 动态渲染 | WebView 加载 SPA 等动态页面 |
| ✅ 人工验证 | WebView 手动登录/过验证码 |
| ✅ 定时采集 | WorkManager 支持间隔/每日定时/WiFi条件 |
| ✅ 批量任务 | 多任务管理，一键运行 |
| ✅ 数据导出 | JSON + Excel (xlsx) |
| ✅ 反爬策略 | UA 轮换 + 请求延迟 + 代理支持 |
| ✅ 数据统计 | 采集量/任务数统计 |

---

## 📦 项目结构

```
android/
├── app/build.gradle.kts           # 依赖: Room, Jsoup, OkHttp, POI, WorkManager
└── src/main/java/com/webscraper/
    ├── MainActivity.kt
    ├── data/                      # 数据层
    │   ├── db/                    # Room Database + DAO
    │   ├── entity/                # 实体类
    │   ├── model/                 # UI 数据模型
    │   └── repository/            # 数据仓库
    ├── engine/                    # 采集引擎
    │   ├── ScraperEngine.kt       # 核心采集器 (OkHttp)
    │   ├── WebViewRenderer.kt     # WebView JS 渲染
    │   ├── HtmlParser.kt          # Jsoup CSS/XPath 解析
    │   ├── AntiCrawlManager.kt    # 反爬策略
    │   ├── JsBridge.kt            # WebView JS 桥接
    │   ├── SelectorGenerator.kt   # CSS 选择器自动生成
    │   └── InjectedJS.kt          # 注入的 JavaScript
    ├── scheduler/                 # 定时调度
    │   ├── ScrapeScheduler.kt     # WorkManager 调度
    │   └── ScrapeWorker.kt        # 后台 Worker
    ├── export/                    # 导出
    │   ├── JsonExporter.kt
    │   └── ExcelExporter.kt
    ├── viewmodel/                 # ViewModel
    └── ui/                        # Jetpack Compose UI
        ├── theme/
        ├── navigation/            # 底部导航
        ├── components/
        └── screens/
            ├── TaskListScreen.kt      # 任务列表
            ├── CreateTaskScreen.kt    # 新建任务
            ├── VisualSelectorScreen.kt # 视觉框选 ⭐
            ├── TaskDetailScreen.kt    # 任务详情
            ├── DataStatsScreen.kt     # 数据统计
            └── SettingsScreen.kt      # 设置
```

---

## 🎯 核心交互流程

```
1. 点击 "+" 创建任务
2. 输入任务名称 + 目标网址
3. 点击"打开页面，框选采集内容"
4. WebView 加载目标网页
5. 点击"选列表"按钮 → 点击页面中的一个商品
6. App 自动识别所有同类商品并高亮
7. 点击"选字段"按钮 → 依次点击标题、价格、图片
8. 点击"保存规则"
9. 返回任务列表 → 点击任务 → 点击"开始采集"
10. 采集完成 → 导出 JSON/Excel
```

---

## 🐳 无需部署

这是一个纯 Android App，不需要任何服务器。
直接编译 APK 安装到手机即可使用。

---

## 💻 本地开发

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34

### 编译

```bash
cd android
./gradlew assembleDebug
```

APK 输出: `android/app/build/outputs/apk/debug/app-debug.apk`

### 运行测试

```bash
cd android
./gradlew test
```

---

## 🔄 CI/CD

### Android CI (`android-build.yml`)

- 触发: push/PR 到 main/develop
- 流程: Checkout → JDK 17 → Gradle Build → Upload APK Artifact
- 产出: `app-debug.apk`

---

## 📋 采集规则说明

### Selector 类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `css` | CSS 选择器（推荐） | `div.product > h2.title` |
| `xpath` | XPath 表达式 | `//div[@class='product']/h2` |

### Field 类型

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `text` | 提取文本内容 | 标题、价格、描述 |
| `attr` | 提取 HTML 属性 | 图片 src、链接 href |
| `html` | 提取原始 HTML | 富文本内容 |

---

## 📄 License

MIT License
