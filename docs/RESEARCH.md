# 调研：大家怎么骂 B 站搜索

按时间倒序。原文链接保留，引用只做归纳。

## 2026-11-22 · 开源实现复核：误删、软过滤与版本探测

本节查看公开仓库的源码与 Issue；项目 README 中的“零误杀”、准确率等描述只视作作者主张，没有独立基准就不当作测量结论。仓库活跃时间及许可也按查阅时 GitHub 显示记录，后续可能变化。

### 值得试用或移植的产品想法

- **软过滤 / 复核路径**：[NEPHERAovo/bilibili-search-fix](https://github.com/NEPHERAovo/bilibili-search-fix/blob/3a0046c87bde1d70843341e2eee882ccc1453803/bilibili-search-fix.user.js#L13-L29) 提供“直接隐藏”与“模糊标记、悬停还原”两种展示策略；它通过底层请求数据和 DOM 检查协同处理，并按当前关键词清除内部相关性集合。这提供了一个可测试的方向：模块侧将来可加“只标记待复核”或“低置信度单独呈现”。Android Hook 不能照搬悬停；现有拦截点是在 App 渲染前过滤响应，移除结果后模块没有相应 UI 来恢复。
- **有界词/标签配置**：该脚本将弱关联标签池限制为 5/10/15/20 条，并在计算时排除泛化标签、按单条视频的标签数量做降权。[实现位置](https://github.com/NEPHERAovo/bilibili-search-fix/blob/3a0046c87bde1d70843341e2eee882ccc1453803/bilibili-search-fix.user.js#L158-L189)。可供未来“已知同义词/圈内别称”规则参考，不过现有宿主卡片没有已证实的完整标签集；不能直接套用这个标签共现算法或 README 宣称的效果。
- **逐视频复核样本**：[同项目](https://github.com/NEPHERAovo/bilibili-search-fix/blob/3a0046c87bde1d70843341e2eee882ccc1453803/bilibili-search-fix.user.js#L148-L155)记录标题、ID、字段命中来源和删除原因。本模块 1.1.0 已借鉴其中“原因 + 样本”的诊断方向，但更严格地在状态广播里限制为五条样本，不全量持久化结果。
- **用户词表的误杀回归**：[Bilibili Search Purify issue #1](https://github.com/houliabc/bilibili-search-purify/issues/1) 是针对脚本 v3.1 的历史报告：用户称“榨菜肉丝面”被内置 `/肉丝/` 词条几乎整页误删，并指出当时查询拆分过窄。查阅时的 v3.7 源码已加入黑名单词匹配当前查询时跳过该条的保护；所以这条 Issue 是“为什么要加冲突保护”的证据，不代表当前主分支仍有相同缺陷。脚本维护者在评论中推荐了 `bilibili-search-fix`。可借鉴的是为屏蔽词加样例回放、明确报告实际命中，并检查用户词条是否与当前查询冲突。现有配置支持用户自定义词，也同样应补“此词会命中当前查询 / 标题”的可解释预览。
- **把响应批次与查询会话分开**：多个网页项目会在每次响应 / DOM 变化时重新检查卡片。当前状态页已将计数明确标为“批结果”；将来如果要表示“一次搜索”，可用规范化 keyword + 响应类型 + 查询时间窗口归并刷新、重试和分页，避免把页面生命周期混成搜索次数。需要单独实测，不能只凭浏览器 MutationObserver 方案推断 Android RPC 时序。

### 兼容性与协议参考

- **Java Getter 和 Kotlin Moss 双通道**：[BBZQ `SearchPurifyHook`](https://github.com/HSSkyBoy/BBZQ/blob/cacde6e70d6aa057b8e516538ef1f4f102c6e14f/app/src/main/java/io/github/bbzq/feats/hook/home/SearchPurifyHook.kt#L33-L65) 同时 hook `SearchAllResponse.getItemList` 与 `KSearchMoss.searchAll`，并在 Kotlin Moss 回调中替换 response。再次印证当前链路需要逐版本检查通道，但不能证明本模块未覆盖的 B 站版本一定走其中某一条。
- **线格式往返、保持不变快路及失败放行**：[BBZQ `KotlinMossBridge`](https://github.com/HSSkyBoy/BBZQ/blob/cacde6e70d6aa057b8e516538ef1f4f102c6e14f/app/src/main/java/io/github/bbzq/feats/hook/network/KotlinMossBridge.kt#L14-L21) 先编码 Kotlin protobuf、通过 Java `parseFrom`/`toByteArray` 往返以构造可编辑对象，序列化后若未变化就原样返回，否则解码并再次 round-trip 校验。其 callback proxy 在转换失败时转发原响应。当前模块暂未做 Kotlin Moss 请求/响应桥接；这段是可参考设计，不是可直接拷贝的依赖，需先了解代码结构、增加纯 JVM codec/代理测试，再真机验证回调顺序、取消、错误、重复回调和线格式未知字段。
- **版本钉定 + 真机探测**：[bilibili-adfree-patches](https://github.com/Chiyang001/bilibili-adfree-patches/tree/29be3724cb75c36b549193598d3cd1fc06b3e154) 将补丁固定到 APK / dex 哈希，构建脚本先验证原件哈希，设备脚本记录有限覆盖范围，并明确不保证所有服务端变体。该仓库改二进制且未声明可复用的开源许可，不移植实现；可借鉴其验证纪律，为模块保留“B 站版本 × 搜索入口 × 命中 Hook × 返回结果”兼容表，避免用 API 大版本概括覆盖率。
- **独立客户端的数据能力差异**：[PiliPlus 搜索控制器](https://github.com/bggRGjQaUbCoE/PiliPlus/blob/217fb54f51f6764176319bbdedab2a7522d75401/lib/pages/search_panel/controller.dart#L28-L45) 会将排序、时长、分区和发布时间起止作为明确搜索参数发送；其视频模型也有 tag、ctime、pubdate 字段。那是 API 客户端可以直接控制请求的能力，不能推导出官方 App 的本地 Moss response hook 同样能按时间过滤或重新召回结果。本模块应继续将无法确认的字段列为限制。

### 不直接采用的机制与许可证备注

- **浏览器响应篡改与 DOM 监听**：[NEPHERA 源码](https://github.com/NEPHERAovo/bilibili-search-fix/blob/3a0046c87bde1d70843341e2eee882ccc1453803/bilibili-search-fix.user.js#L303-L355) 包装网页 `fetch` / XHR，[并另装 DOM MutationObserver](https://github.com/NEPHERAovo/bilibili-search-fix/blob/3a0046c87bde1d70843341e2eee882ccc1453803/bilibili-search-fix.user.js#L441-L449)。它依赖 Web API、字段布局和独立返回 JSON；不适用于本项目的 protobuf 消息反射 Hook。
- **“白屏即恢复所有卡”**：[B 站搜索结果纯净版 v3.7](https://github.com/houliabc/bilibili-search-purify/blob/644938cc28d2dd034aaf262615e42f291f7d1fdb/bilibili-search-purify.user.js#L198-L204) 在统计可见结果为零时无条件恢复所有视频卡，避免空白，但同时撤销该批过滤。对本模块而言这会静默放行被评分为无关的内容，不符合当前“空页优于垃圾页”原则。若空页造成问题，应说明触发类别并给出临时关闭开关，而不是在后台推翻用户所选规则。
- **自动同义标签共现扩展的边界**：NEPHERA 源码让其“强相关”判断通过的卡片累积已有标签，按出现频次建立当前会话 Top-K 池，再对其他卡片做标签 / 标题包含匹配；我没有在该实现中看到 README 所述的独立跨视频共现验证。实际上，在每视频标签数不超过 7 的情况下，单张卡就能给每个标签累积 1 分，超过源码 0.35 的入池门槛，因此不要求多个视频交叉验证。源码的强相关判定还接受官方高亮和任意查询 token 在标题、标签、作者或简介中命中，不能等同于严格标题匹配。词项共现本身不证明语义同义，标签也可能噪声。官网搜索卡缺 tag 的情况下此池更不可用。若将来采纳，先做明确 opt-in、会话级别、不持久化，并以用户判断的固定查询样本测误留 / 误删；“零闪烁 / 零误杀”等 README 自述不视为测量结论。
- **许可**：NEPHERA 项目标示 MIT；BBZQ 仓库含木兰公共许可证 v2；PiliPlus 为 GPL-3.0；`Bilibili-Block-List` 是 CC BY-NC 4.0 的弹幕规则列表，与搜索卡屏蔽不同；`bilibili-adfree-patches` 未声明可复用许可。以上只作参考来源；借鉴行为模式与复制代码 / 词库规则的许可要求不同。查阅时 BBZQ 最近提交为 2026-11-21，PiliPlus 为 2026-11-14，NEPHERA 为 2026-11-08；时间与项目热度都不能作为兼容性或效果的证明。

### 建议优先级

1. **低风险、近期**：把过滤规则验证与回放体验补齐；为屏蔽词 / 白名单显示匹配预览，在纯 JVM 回放测试中继续增加用户真实搜索词样例，并保留诊断样本上限。
2. **中风险、先设计后实现**：探索“软过滤”作为独立的可选 UI affordance；确认在官方 Activity 可控范围内是否存在支持恢复的稳定入口，否则维持仅显示删除理由，不承诺恢复卡片。
3. **高风险、独立验证**：Kotlin Moss / response codec 双通道适配；建立按 B 站精确版本和综合/分类搜索入口记录的真机兼容表，并先给 `KotlinMossBridge` 类型分离设计和 round-trip 测试方案。
4. **暂缓**：请求改写、静态社区黑名单自动订阅、仅凭播放量替代相关性或默认空页恢复；它们分别涉及签名/风控、词库误杀、搜索目标改变和策略静默失效。

## 实现参考

- **BBZQ**（https://github.com/HSSkyBoy/bbzq）：Kotlin + Modern API 102 的 B 站增强模块。它证明新版 App 搜索走 Moss gRPC，hook 点是 `SearchAllResponse.getItemList` / `SearchByTypeResponse.getItemsList`；广告卡是 `CM` / `PURCHASE`。本模块只取搜索链路，其余功能不做。
- **Kirosca 脚本**（https://www.bilibili.com/opus/1231629834092806164）：网页端油猴脚本 + PiliPlus 定制版，提供 `- # @` 语法。脚本在 fetch 层删广告、缓存 tag，DOM 层四道闸过滤。

## 2026-10-27 · B站动态：社区已有网页 + 移动端方案

作者 Kirosca 发布了油猴脚本和 PiliPlus 定制版，支持 `关键词 -排除 #标签 @UP` 语法。

- 脚本在 fetch 层拦截 `/wbi/search`、`/search/type`、`/search/all`，删广告并缓存 tag；DOM 层四道闸过滤。
- `getCleanApiKeyword()` 会把 `-排除词` 从请求 keyword 里去掉，再本地过滤。
- 缺点：纯规则，需要用户知道排除什么；删太多会出现整页空白。
- 动态：https://www.bilibili.com/opus/1231629834092806164
- 脚本：https://scriptcat.org/zh-CN/script-show-page/7723
- PiliPlus：https://github.com/bggRGjQaUbCoE/PiliPlus

## 2026-10 · NGA：无痕模式也躲不开

> 130 万播放的第一个预告片不在前头，2 天前的特别预告在第一页末尾，还夹杂一堆标题和 tag 里都没有「复仇者联盟」的东西，其中三个完全无关：影之刃零、华尔街之狼、泥面人。

- 游客/无痕模式没有「最多播放 / 最新发布 / 筛选」入口。
- 搜索词匹配不到的内容照样出现。
- 来源：https://bbs.nga.cn/read.php?tid=47785439

## 2026-05 · NGA：个性化污染 + 时间排序失效

> 很多结果是按你最近观看的类似视频以及某些视频热度排序的，除此之外还有广告、买量视频、争议视频，有时结果与搜索词完全不相关。

> 默认排序能看到今天上午 10 点发的视频；切到按时间排序，这个视频反而不见了，昨天的却在。

> 搜索词太冷门、没有相关视频时，塞大量无关视频。

- 子分类被取消，搜游戏全是 MC 和英雄联盟。
- 来源：https://bbs.nga.cn/read.php?tid=46885392
- 来源：https://ngabbs.com/read.php?tid=46765836

## 2026 · TrueSight：搜不到、搜到无关、排序乱

> 搜 A，出来 B、C、D，甚至还有 Z，就是没有 A。

- 同一关键词 APP 搜不到、网页能搜到。
- 新视频排在几年前的老视频后面。
- 来源：https://tsight.io/articles/6517161

## 2024 · V2EX：搜索只覆盖标题和作者标签

> 一个视频，封面上的标题、下面文字标题、讲稿内容可以完全不挨边；搜索一般只能搜文字标题和作者预设的关键字。

- 结果：用户记得内容，但搜不回来。
- 来源：https://global.v2ex.co/t/1098485

## 2021 · V2EX：描述里带关键词就能进结果

> 点开其中一个视频，发现是视频描述中带有「小窗」两个字，所以进入了搜索结果。

- 按播放量排序时尤其明显：一堆只在简介里蹭到词的内容。
- 多年来没有关键词排除功能。
- 来源：https://v2ex.com/t/796339

## 归纳出的 6 个问题

| 问题 | 现有方案 | 本模块对策 |
|---|---|---|
| 结果与关键词零关联 | 无 | 标题 bigram 相关性硬过滤 |
| 广告、买量、争议视频 | 网页脚本可屏蔽 | `CM` / `PURCHASE` 卡直接删 |
| 个性化排序压过相关性 | 无 | 本地按文本相关性重排（M5） |
| 时间排序反而更旧 | 无 | gRPC 卡片无 pubdate，暂不支持 |
| 简介蹭词 | 无 | 简介权重降到标题的 0.5 |
| 冷门词硬塞结果 | 无 | 宁可空页，也不展示垃圾 |
| 用户想精确排除词/标签/UP | 油猴脚本、PiliPlus | 移植 `- # @` 语法，和自动打分叠加 |
