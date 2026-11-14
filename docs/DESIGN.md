# 设计

## 目标

在官方 B 站 App 内提高搜索结果的相关性，只改搜索内容，不碰首页、播放页、动态和账号行为。

非目标：

- 不做账号级操作（拉黑、举报、点不感兴趣）。
- 不伪造请求、不绕过风控、不额外抓取。
- 不接管 UI 布局。

## 原则

1. **空结果优于垃圾结果**：相关度不够就删，不拿推荐位凑数。
2. **只读响应**：不发起额外请求，零风控风险。
3. **版本无关**：不依赖混淆类名，只依赖 protobuf 生成类。
4. **可回放**：每条被删内容输出 title / 分数 / 原因，方便调阈值。

## 数据流

```
BiliBili 9.0+
  └─ Moss gRPC
       ├─ SearchAllResponse.getItemList()
       └─ SearchByTypeResponse.getItemsList()
            └─ [Hook] 拿到 List<Item> + response.getKeyword()
                 ├─ 总开关 / 白名单 -> 直接返回
                 ├─ CM / PURCHASE / 推荐卡 -> 删
                 ├─ 只保留视频 -> 非 AV 卡删
                 ├─ AV -> 读 title/author/mid/desc/play
                 │        ├─ 屏蔽词 / 屏蔽 UP / 最低播放量 / 去重
                 │        └─ SearchFilter.evaluate()
                 └─ 其他 -> 保留
            └─ 可选本地重排 -> 返回新 List
```

### 为什么不用 OkHttp JSON

B 站 9.0+ 的 App 搜索走 Moss gRPC，JSON 接口是网页/旧版本的路径。BBZQ 的实现已经证明正确的 hook 点是：

- `com.bapis.bilibili.polymer.app.search.v1.SearchAllResponse#getItemList`
- `com.bapis.bilibili.polymer.app.search.v1.SearchByTypeResponse#getItemsList`

## Hook 点

| 点 | 作用 | 备注 |
|---|---|---|
| `onPackageReady` | 拿真实 App ClassLoader | Modern API 102 官方入口 |
| `SearchAllResponse.getItemList` | 综合搜索 | 返回过滤后的 List |
| `SearchByTypeResponse.getItemsList` | 分类搜索 | 视频/用户/直播 Tab |
| `XposedService.getRemotePreferences` | 开关/阈值 | 模块 App 写，B 站进程读；框架托管 |
| `StatusReceiver` (explicit broadcast) | Hook/搜索统计 | B 站进程发广播，模块 App 写本地 prefs |

## 相关性算法

### 归一化

- NFKC（全角转半角）
- 小写
- 去掉空白、标点、符号
- 去掉标题里的 `<em class="keyword">` 高亮标签，但保留高亮次数

### 查询展开

- 中文：整串 + 相邻 bigram（`命运石之门` -> 命运/运石/石之/之门）
- 英文/数字：按词边界
- 单字查询：只保留整串

### 打分

| 信号 | 分数 | 说明 |
|---|---|---|
| 标题高亮 `<em>` | 0.95 | B 站自己标了命中 |
| 标题包含完整查询 | 1.00 | 最强信号 |
| 标题 bigram 覆盖率 | 0-1.0 | 主信号 |
| tag/简介覆盖率 × 0.8 | 0-0.8 | 辅助 |
| 简介覆盖率 × 0.5 | 0-0.5 | 只蹭简介的降权 |
| 广告卡 | 0 | 直接删 |

默认阈值 0.5。低于阈值删除。UP 主名不参与自动打分。

## 规则语法（移植自社区方案）

```
摄影 -入门 #ai @影视飓风
```

| 语法 | 含义 | 行为 |
|---|---|---|
| `词` | 必含 | 严格模式下标题必须包含全部普通词 |
| `-词` | 排除 | 标题包含即删 |
| `#词` | 标签 | 标签/简介必须包含 |
| `@词` | UP 主 | 作者匹配任一 |

### 混合语义

- **无修饰词**：只跑自动相关性（bigram 容忍语序和空格）。
- **出现任一修饰词**：进入严格模式，普通词必须全部出现在标题；再叠加 `- # @` 硬规则。
- **只有修饰词**：跳过自动打分，只执行规则。

### App 端字段限制

protobuf 的 `SearchVideoCard` 只有 title / author / desc / play / duration / mid，没有 tag 和 pubdate。因此：

- `#标签` 在 App 上回退为「标题或简介包含」。
- 暂不能按发布时间本地排序。
- 播放量存在，M5 可做「相关性 > 播放量」重排。

## 广告卡

`Item.card_item` 的 oneof 里 `cm`（SearchAdCard）和 `purchase`（商品卡）直接删除；`goto == "ad"` 也删。其他类型（用户、直播、番剧、专栏）默认保留，避免破坏搜索页结构。

## 结果策略

处理顺序：

1. 总开关关闭 → 原样返回
2. 搜索词命中白名单 → 原样返回
3. 卡片类型：`CM` / `PURCHASE`、推荐词卡、提示卡
4. 只保留视频模式下，所有非 `AV` 卡删除
5. `AV` 卡：屏蔽词 → 屏蔽 UP → 最低播放量 → 标题去重 → 相关性打分
6. 可选本地重排

### 去重

key = 归一化标题 + `|` + 归一化作者。只去完全相同的标题 + 作者，不做语义相似度，避免误杀同题材不同视频。

### 重排

只对 `AV` 槽位排序：相关性分数降序 → 播放量降序 → 原顺序兜底。非视频卡（用户/直播/番剧）停留在原位置，不会因为排序被挪走。

### 统计

- Hook 每次搜索通过显式广播回传「保留 / 总数 / 删除」。
- `StatusReceiver` 把累计搜索次数和累计删除数写进模块 App 的本地 prefs。
- 状态卡只展示结果，不展示「安装状态未回传」这类实现细节。

## 设置与状态

- 模块 App 在 `Application.onCreate` 注册 `XposedServiceHelper.OnServiceListener`，拿到 `XposedService` 后通过 `getRemotePreferences(SETTINGS_PREFS)` 读写设置。
- B 站进程通过 `XposedModule.getRemotePreferences(SETTINGS_PREFS)` 直接读取同一组设置，不再经过 ContentProvider，也不受模块 App 被 force-stop 影响。
- Hook 状态和搜索统计由 B 站进程通过显式广播发出，模块 App 的 `StatusReceiver` 写入本地 prefs。
- 状态包含：安装数/时间/进程/模块版本、最近搜索、累计搜索/删除数。
- 设置页使用 Material 3 + XML：MaterialToolbar、MaterialCardView、MaterialSwitch、Slider、MaterialButtonToggleGroup、TextInputLayout。
- 根布局处理 system bars insets；开关/滑条即时保存，文本规则失焦或 `onPause` 自动保存；状态卡放在页尾，文案只讲结果。

## 隐私

- 不上传任何数据。
- 不读取账号 Cookie。
- 状态回报只写入本机模块 App 的 SharedPreferences。
- 日志只在本机 logcat，包含搜索词和被删标题（用于调试）。

## 版本适配

- B 站 9.0+（BBZQ 的适配基线）。
- Modern API 102，最低 Android 9（API 28）。
- 只加载 `tv.danmaku.bili` 主进程。
- 底部「相关搜索」区块由独立 UI 渲染，当前不覆盖；`hideRecommend` 只处理 Item 列表里的推荐词/提示卡。
