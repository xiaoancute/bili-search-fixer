# BiliSearch Fixer

> LSPosed 模块：只改 B 站客户端的搜索内容——删掉和关键词毫无关系的视频、广告卡和买量卡。

![build](https://github.com/xiaoancute/bili-search-fixer/actions/workflows/build.yml/badge.svg)

## 问题

B 站搜索的相关性已经烂到众所周知：结果里混着广告卡、买量视频、个性化推荐，以及和关键词 100% 零关联的内容；时间排序甚至会把更新的视频排到后面。游客/无痕模式连排序和筛选入口都没有。

吐槽原帖和社区方案整理在 [docs/RESEARCH.md](docs/RESEARCH.md)，覆盖 NGA、V2EX 2021-2026 的讨论。

## 方案

B 站 9.0+ 的 App 搜索已经不走 HTTP JSON，而是 Moss gRPC protobuf：

```
SearchAllResponse        SearchByTypeResponse
  getItemList()            getItemsList()
        \                      /
         \                    /
          [Hook] 返回过滤后的 List<Item>
                    |
                    ├─ CM / PURCHASE 卡：直接删
                    ├─ AV 卡：标题/作者/简介做相关性 + 规则
                    └─ 其他类型：保留
```

关键点：

- 只 hook 两个 getter，不动 UI、不动请求、不发额外网络请求。
- 综合搜索和分类搜索（视频/用户/直播 Tab）都覆盖。
- 用 Modern API 102，设置通过 `XposedService.getRemotePreferences` 同步。
- Hook 状态/搜索统计写入框架远程文件，模块 App 直接读取。
- 结构参考 [BBZQ](https://github.com/HSSkyBoy/bbzq)，但只保留搜索链路。

## 当前状态

| 能力 | 状态 |
|---|---|
| Modern API 102 入口 + 进程过滤 | ✅ |
| `SearchAllResponse.getItemList` Hook | ✅ 代码完成，待真机验证 |
| `SearchByTypeResponse.getItemsList` Hook | ✅ 代码完成，待真机验证 |
| 广告卡/买量卡删除 | ✅ |
| 标题 bigram 相关性 + `- # @` 规则 | ✅ 有单测 |
| 最低播放量 / 屏蔽词 / 屏蔽 UP | ✅ 有单测 |
| 标题去重 / 只保留视频 / 隐藏推荐卡 | ✅ |
| 本地重排（相关性 → 播放量） | ✅ |
| Material 3 设置页 | ✅ XML + Material Components |
| Hook 状态 + 累计/最近搜索统计 | ✅ |
| 真机回归 | ✅ OnePlus / Android 16 / B站 9.16.1 |

v0.6.0 已把设置同步切到 LSPosed 官方 `XposedService.getRemotePreferences`，状态改走远程文件，不再依赖 ContentProvider，也不会被 force-stop 卡住。真机验证基线：OnePlus PLF110 / Android 16 / B站 9.16.1。

## 真机验证

2026-11-14 实测：

- Modern API 安装 2 个 Hook：综合搜索 + 分类搜索
- `bad apple`：20 条删 10-18 条，零关联标题被移除
- `bad apple -Dongxi`：`Bad Dongxi!!` 命中 `excluded:dongxi`，其余 `title_missing:bad`，最终只剩 2 条精确匹配
- 最低播放量、屏蔽词、只保留视频、去重、本地重排均生效
- 首次冷启动设置竞态已用 preload + 重试修复

## 构建（只用 GitHub Actions）

本仓库**不在本地编译**。推送后 Action 会：

1. 安装 JDK 21、Android SDK 37、Gradle 9.7.1
2. 跑单测
3. 产出 debug + release APK
4. 用固定 key 签名（更新可直接覆盖安装）
5. 上传 artifact

```bash
gh run list --workflow build.yml --limit 1
gh run download --name BiliSearchFixer-release   # 日常安装
gh run download --name BiliSearchFixer-debug     # 需要调试时
```

## 安装

1. 手机安装支持 Modern API 102 的 LSPosed（或 NPatch）。
2. 安装 APK，桌面会出现「BiliSearch Fixer」，打开后可开关过滤、调阈值。
3. 在框架里启用模块。
4. 作用域勾选「哔哩哔哩」(`tv.danmaku.bili`)。
5. 强制停止并重启 B 站，搜索一次。
6. 回到模块设置页点「刷新 Hook 状态」，应显示「安装 2 个 Hook」；搜索后还会显示最近一次搜索保留/删除了多少条。

也可以看日志：

```bash
adb logcat | grep BiliSearchFixer
```

正常会看到：

```
hooked com.bapis.bilibili.polymer.app.search.v1.SearchAllResponse.getItemList
search="原神攻略" kept=12/20 removed=8 ...
```

## 过滤项

| 开关 | 作用 |
|---|---|
| 启用搜索过滤 | 总开关 |
| 删除广告 / 商品卡 | `CM` / `PURCHASE` / `goto=ad` |
| 只保留视频 | 隐藏用户、直播、番剧、专栏等卡片 |
| 隐藏推荐词 / 提示卡 | `RECOMMEND_WORD` / `TIPS` 等 |
| 标题去重 | 同标题 + 同作者的重复结果只留第一条 |
| 本地重排 | 视频卡按相关性降序，再按播放量降序；不移动用户/直播卡的位置 |

规则页还可以设置：

- **最低播放量**：低于该值直接删
- **屏蔽词**：标题包含即删，每行一个
- **屏蔽 UP 主**：名字或 mid，每行一个
- **白名单关键词**：搜索词包含其中任意一个时跳过过滤

强度支持「宽松 30% / 标准 50% / 严格 70%」三个预设，也可以拖动滑条微调。

## 界面

设置页使用 `Material 3 + XML`：

- `MaterialToolbar` + `NestedScrollView`
- `MaterialCardView` 分组
- `MaterialSwitch` / `Slider` / `MaterialButtonToggleGroup`
- `TextInputLayout` 输入规则
- 自动适配深色模式

## 规则语法

搜索词里可以直接带：

```
关键词 -排除词 #标签 @UP主

摄影 -入门 #ai @影视飓风
```

| 语法 | 行为 |
|---|---|
| `词` | 必含 |
| `-词` | 标题包含即删 |
| `#词` | 标签必须包含；App 响应没有 tag，回退到简介 |
| `@词` | 作者匹配任一 |

无修饰词时只跑自动相关性；出现任一修饰词时进入严格模式，普通词必须全部出现在标题。

## 已知限制

- gRPC 搜索卡片没有 tag 字段，`#` 规则只能回退到简介匹配。
- gRPC 搜索卡片没有 pubdate，暂时不能做本地时间排序。
- 标题去重按「归一化标题 + 作者」判断，不做语义相似度。
- 重排只在视频卡槽位内排序，不会把用户/直播卡挪走。
- 底部「相关搜索」是独立区块，当前 hook 不覆盖；`隐藏推荐卡` 只处理结果列表里的推荐词/提示卡。
- 搜索词仍会原样发给 B 站，`-排除词` 只能本地过滤，不能改变服务端召回。请求改写要处理签名，放在后面验证。
- 适配 B 站 9.0+；更老的版本可能仍走 JSON，当前不覆盖。

## 路线图

- [x] M1 工程骨架 + CI
- [x] M1.5 规则语法 `-排除 #标签 @UP`
- [x] M2 迁移 Modern API + gRPC 搜索 Hook
- [x] M3 设置页 + Hook 状态回报
- [x] M4 Material 3 界面 + 多项过滤 + 本地重排
- [x] M5 真机验证 + 冷启动设置竞态修复
- [ ] M6 请求改写（先验证签名）

## 参考

- [BBZQ](https://github.com/HSSkyBoy/bbzq) — Modern API、Moss Hook、搜索卡片结构的直接参考
- [BiliRoamingX](https://github.com/zjns/BiliRoamingX) — Tribe ClassLoader / DexKit 思路
- [bilibili-cleaner](https://github.com/festoney8/bilibili-cleaner) — 网页端过滤规则模型
- [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) — protobuf 字段
- [B站去掉搜索无关视频](https://www.bilibili.com/opus/1231629834092806164) — `- # @` 语法来源

## 免责声明

仅供个人学习与本地使用。模块只读取搜索响应，不向任何第三方服务器发送数据，不修改账号状态。与哔哩哔哩官方无关。

## License

GPL-3.0
