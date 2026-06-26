# 掌心热榜 HandHot

> **版本**：V1.1 (含优化补充)  
> **最后更新**：2026-06-26  
> **状态**：✅ 开发中  
> **开源协议**：MIT License

个人轻量级资讯聚合工具，手动刷新获取多个网站热榜/板块的未读内容。基于 **CSS 选择器** 自定义抓取任意网页，填补 RSS 生态的空白。

---

## 一、项目概述

| 项目 | 内容 |
| :--- | :--- |
| **产品定位** | 个人轻量级资讯聚合工具，手动刷新获取多个网站热榜/板块的未读内容 |
| **目标用户** | 需要集中追踪多个网站热榜，且不希望被后台推送打扰的用户 |
| **部署平台** | Android 手机（优先适配小米MIUI） |
| **技术方案** | 原生 Android App（Kotlin + Jetpack Compose + Material3） |

---

## 二、技术栈

| 层 | 技术 | 说明 |
|:---|:---|:---|
| UI | Jetpack Compose + Material3 | 声明式 UI，原生滑动手势支持 |
| 架构 | MVVM + Hilt DI | ViewModel + Repository + UseCase |
| 数据库 | Room | 本地 SQLite ORM |
| 网络 | OkHttp 4.x | UA 池、Cookie 拦截器、反爬 |
| 解析 | Jsoup | CSS 选择器 HTML 解析 |
| 图片 | Coil 3.x | Kotlin 原生图片加载 + LRU 缓存 |
| 存储加密 | AndroidX Security | EncryptedSharedPreferences |
| 后台任务 | WorkManager | 定时数据清理 |
| 构建设置 | Gradle Kotlin DSL + Version Catalog | 现代化构建配置 |

---

## 三、核心功能清单

| 模块 | 优先级 | 说明 |
| :--- | :--- | :--- |
| 数据源管理 | P0 | 增删改查数据源，CSS选择器配置，选择器测试 |
| 下拉刷新抓取 | P0 | 用户主动刷新，并发抓取，增量去重，域名级冷却 |
| 内容列表展示 | P0 | 标题+50字摘要+封面图，按源分组，星标置顶 |
| 已读/未读管理 | P0 | 自动+手动标记已读，"全部已读"可撤销（Snackbar） |
| 内置浏览器 | P0 | WebView 查看详情，延迟标已读，WebView 安全加固 |
| 图片缓存 | P1 | Coil 磁盘缓存，OG:image 智能降级，Favicon 多路径查找 |
| 登录态管理 | P1 | WebView 登录拦截 Cookie，EncryptedSharedPreferences 加密存储 |
| 星标/稍后读 | P2 | 右滑标星，不受已读清理影响 |
| 配置导入导出 | P2 | JSON 格式备份/恢复，排除 Cookie |
| 夜间模式 | P2 | 跟随系统/浅色/深色 |

---

## 四、详细需求

### 4.1 数据源管理

#### 添加数据源表单

| 字段 | 必填 | 说明 | 示例 |
| :--- | :--- | :--- | :--- |
| 源名称 | ✅ | 自定义显示名称 | "知乎热榜" |
| 目标URL | ✅ | 热榜/板块页面地址 | `https://www.zhihu.com/hot` |
| CSS选择器-标题 | ✅ | 提取标题 | `a.QuestionLink` |
| CSS选择器-摘要 | ❌ | 提取摘要（取文本前50字） | `div.HotItem-excerpt` |
| CSS选择器-封面图 | ❌ | 提取图片URL（留空则自动提取 OG:image） | `img.HotItem-img` |
| CSS选择器-链接 | ✅ | 提取详情页链接 | `a.QuestionLink` |
| CSS选择器-时间 | ❌ | 提取发布时间（🆕 V1.1 新增） | `time.HotItem-time` |
| 是否需要登录 | ❌ | 开关，默认关闭 | 开启后走登录流程 |

#### 预置数据源（外置 JSON）

预置源配置存储在 `assets/default_sources.json`，改版时无需发版：

```json
{
  "version": 1,
  "sources": [
    {"name": "知乎热榜", "url": "https://www.zhihu.com/hot", ...},
    {"name": "微博热搜", "url": "https://s.weibo.com/top/summary", ...},
    {"name": "V2EX 热榜", "url": "https://www.v2ex.com", ...},
    {"name": "少数派热门", "url": "https://sspai.com", ...},
    {"name": "百度贴吧热榜", "url": "https://tieba.baidu.com/hottopic/browse/topicList", ...}
  ]
}
```

---

### 4.2 下拉刷新抓取

#### 刷新流程

```
用户触发刷新
  → 检查网络 (无网络→Toast)
  → 检查源级冷却 (60s) + 域名级冷却 (10s)  ← 🆕 V1.1 新增域名级
  → 自适应并发 (WiFi 5 / 4G 3 / 3G 1)     ← 🆕 V1.1 网络质量自适应
  → OkHttp GET + UA 池伪装
  → Jsoup CSS 选择器解析
  → OG:image 降级提取封面图                ← 🆕 V1.1 智能封面图
  → hash = MD5(title + link) 去重          ← 🆕 V1.1 改用 title+link
  → Room 增量写入
  → UI 更新
```

#### 增量去重逻辑（🆕 V1.1 优化）

| 判断条件 | 处理 |
| :--- | :--- |
| `MD5(title + link)` 已存在 | **跳过**（即使摘要不同也跳过——链接是唯一标识） |
| hash 不存在 | 插入新记录，标记为未读 |
| 某源解析为空 | 保留旧数据，fail_count+1（可能选择器失效） |
| 某源状态码 403/429/503 + 含 captcha 关键词 | 标记为验证码拦截 | ← 🆕 V1.1 双重判定 |

#### 反爬策略（🆕 V1.1 增强）

- **域名级冷却**：同域名下所有源共享 10 秒冷却计时器
- **并发自适应**：根据 `ConnectivityManager` 动态调整并发数
- **响应体限制**：最大 512KB，防止 OOM

---

### 4.3 已读/未读管理

#### "全部已读"可撤销（🆕 V1.1 优化）

点击"全部已读" → Snackbar "已标记 N 条为已读" + 「撤销」按钮（3 秒内有效）

#### 延迟标记可配置（🆕 V1.1 新增）

| 设置项 | 选项 | 默认值 |
| :--- | :--- | :--- |
| 详情页自动标已读延迟 | 关闭 / 1秒 / 2秒 / 5秒 | 2秒 |

---

### 4.4 封面图策略（🆕 V1.1 增强）

封面图提取优先级：

```
1. 用户自定义选择器（selector_image 字段）
2. <meta property="og:image"> — 自动提取，无需用户配置
3. /favicon.ico 占位
```

Favicon 获取路径（🆕 V1.1 多路径）：

```
1. <link rel="icon" href="...">
2. <link rel="shortcut icon" href="...">
3. https://domain.com/favicon.ico
4. Google Favicon API fallback
```

---

### 4.5 WebView 安全加固（🆕 V1.1 新增）

```kotlin
settings.apply {
    // PRD 原有的
    javaScriptEnabled = true
    domStorageEnabled = true
    // 🆕 安全加固
    allowFileAccess = false
    allowContentAccess = false
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false
    mixedContentMode = MIXED_CONTENT_NEVER_ALLOW
}
```

---

## 五、包结构

```
com.handhot.app/
├── HandHotApplication.kt          # Hilt 入口
├── MainActivity.kt                # 唯一 Activity (Compose)
├── di/
│   └── AppModule.kt               # Hilt 依赖注入
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room 数据库
│   │   ├── dao/
│   │   │   ├── FeedSourceDao.kt
│   │   │   └── FeedItemDao.kt
│   │   └── entity/
│   │       ├── FeedSource.kt
│   │       └── FeedItem.kt
│   ├── remote/
│   │   ├── fetcher/
│   │   │   ├── FeedFetcher.kt     # 接口 + PublicFetcher + LoginFetcher
│   │   │   └── FetcherFactory.kt  # 工厂 + OkHttpProvider
│   │   ├── parser/
│   │   │   └── HtmlParser.kt      # Jsoup 封装 (OG + Favicon)
│   │   └── model/
│   │       └── FetchResult.kt
│   ├── repository/
│   │   └── FeedRepository.kt      # 数据仓库 (去重 + 并发调度)
│   └── cookie/
│       ├── CookieManager.kt       # EncryptedSharedPreferences
│       └── CookieInterceptor.kt   # OkHttp 拦截器
├── domain/
│   └── usecase/
│       ├── RefreshFeedsUseCase.kt
│       ├── MarkReadUseCase.kt
│       ├── ToggleStarUseCase.kt
│       └── CleanOldDataUseCase.kt
├── ui/
│   ├── navigation/
│   │   └── NavHost.kt
│   ├── main/
│   │   ├── MainScreen.kt          # 首页 (列表 + Swipe + PullRefresh)
│   │   └── MainViewModel.kt
│   ├── source/
│   │   ├── AddSourceScreen.kt     # 添加/编辑源 (表单 + 选择器测试)
│   │   └── SourceViewModel.kt
│   ├── detail/
│   │   └── WebViewScreen.kt       # 内置浏览器 (安全加固)
│   ├── settings/
│   │   ├── SettingsScreen.kt      # 设置页面
│   │   ├── SettingsViewModel.kt   # DataStore 偏好
│   │   ├── AboutScreen.kt         # 关于 + 免责 + 隐私
│   │   └── CleanupWorker.kt       # WorkManager 定时清理
│   └── theme/
│       └── Theme.kt               # Material3 浅/深色主题
└── utils/
    ├── HashUtils.kt               # MD5 去重
    ├── NetworkUtils.kt            # 网络检测 + 并发自适应
    └── UserAgentPool.kt           # UA 随机池 + 域名提取
```

---

## 六、数据库设计

### FeedSource 表

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | INTEGER PK | 自增 |
| name | TEXT | 显示名称 |
| url | TEXT UNIQUE | 目标 URL |
| selector_title | TEXT | CSS 选择器 |
| selector_summary | TEXT? | CSS 选择器 |
| selector_image | TEXT? | CSS 选择器 |
| selector_link | TEXT | CSS 选择器 |
| selector_time | TEXT? | 🆕 V1.1 新增 |
| need_login | INTEGER | 0/1 |
| enabled | INTEGER | 0/1 |
| last_fetch_time | INTEGER | 时间戳 |
| fail_count | INTEGER | 连续失败次数 |
| last_error | TEXT? | 最近错误 |
| sort_order | INTEGER | 排序权重 |
| created_at | INTEGER | 创建时间 |

### FeedItem 表

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | INTEGER PK | 自增 |
| source_id | INTEGER FK | → FeedSource.id |
| title | TEXT | 标题 |
| summary | TEXT? | 摘要 (50字) |
| cover_image_url | TEXT? | 封面图 |
| link | TEXT | 详情链接 |
| hash | TEXT UNIQUE | 🆕 V1.1 改为 MD5(title+link) |
| pub_time | INTEGER | 发布时间 |
| is_read | INTEGER | 0/1 |
| is_starred | INTEGER | 0/1 |
| created_at | INTEGER | 抓取时间 |

---

## 七、V1.1 优化清单（本次补充）

| # | 优化项 | 优先级 | 影响 |
|:---|:---|:---|:---|
| 1 | **增加时间选择器字段** | P0 🔴 | 排序规则可正常工作 |
| 2 | **hash 改为 title+link** | P0 🔴 | 消除重复条目误判 |
| 3 | **域名级冷却 (10s)** | P1 🟡 | 防止同域名多源被封 IP |
| 4 | **智能封面图降级 (OG:image)** | P1 🟡 | 减少用户配置成本 |
| 5 | **Favicon 多路径查找** | P1 🟡 | 封面占位不空白 |
| 6 | **"全部已读"可撤销 (Snackbar)** | P1 🟡 | 防用户懊悔 |
| 7 | **网络质量自适应并发** | P2 🟢 | 弱网可用性 |
| 8 | **WebView 安全加固** | P2 🟢 | 长期安全 |
| 9 | **验证码双重判定 (关键词+状态码)** | P2 🟢 | 减少误判 |
| 10 | **选择器外置 JSON** | P2 🟢 | 快速迭代不改版 |
| 11 | **详情页标已读延迟可配置** | P2 🟢 | 用户体验灵活 |

---

## 八、构建 & 安装

### 本地构建

```bash
# 需要 Android Studio Hedgehog+ 或 JDK 17 + Android SDK 35
git clone <repo-url>
cd HandHot
./gradlew assembleDebug
# APK 位于 app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions 自动构建

每次 push 到 main 分支，CI 自动构建 debug + release APK，可在 Actions → Artifacts 中下载。

---

## 九、免责声明

本应用所有数据均来自公开网络，仅用于个人学习，版权归原网站所有。请勿高频抓取或商业使用。

**隐私说明**：本应用不包含任何网络上报、统计 SDK 或遥测。所有数据（Cookie、已读状态、源配置）仅存储于本地，不会上传至任何服务器。

---

## 十、开源协议

MIT License — 完全自由使用、修改和分发。
