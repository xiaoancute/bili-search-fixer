# 设计

## 目标

在不修改 B 站 APK、不依赖其混淆类名的前提下，提高 Android 客户端搜索结果的相关性。

非目标：

- 不做账号级操作（拉黑、举报、点不感兴趣）。
- 不伪造请求、不绕过风控、不额外抓取。
- 不接管 UI 布局。

## 原则

1. **空结果优于垃圾结果**：相关度不够就删，不拿推荐位凑数。
2. **只读响应**：v1 不发起任何额外请求，零风控风险。
3. **版本无关**：不写死 `data.item`，递归识别「长得像搜索卡片」的数组。
4. **可回放**：每条被删内容输出 title / 分数 / 原因，方便调阈值。

## 数据流

```
BiliBili App
  └─ OkHttpClient.newCall(Request)
       └─ [Hook] after: 记录 / 包装 Call
            └─ Response
                 ├─ URL 不命中 -> 原样返回
                 └─ URL 命中 /x/v2/search*
                      ├─ body -> String
                      ├─ SearchJsonRewriter.rewrite()
                      │    ├─ 递归遍历 JSON
                      │    ├─ 识别 item 数组
                      │    ├─ SearchFilter.evaluate()
                      │    └─ 删除 / 保留
                      └─ ResponseBody.create() 重建 Response
```

## Hook 点

| 点 | 作用 | 备注 |
|---|---|---|
| `Application.attach / attachBaseContext` | 拿真实 ClassLoader | B 站有 Tribe 插件化，`lpparam.classLoader` 未必够 |
| `okhttp3.OkHttpClient.newCall` | 拿到所有请求 | 用 `afterHookedMethod` 只读，不改变返回值（M1） |
| `okhttp3.ResponseBody.create` | 重建响应体 | M2 通过反射调用 |
| `okhttp3.Response.Builder.body` | 替换 body | M2 |
| DexKit | 定位被混淆的 okhttp/模型类 | M2 引入，结果按 versionCode 缓存 |

## 相关性算法

### 归一化

- NFKC（全角转半角）
- 小写
- 去掉空白、标点、符号

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
| tag 覆盖率 × 0.8 | 0-0.8 | 辅助 |
| 简介覆盖率 × 0.5 | 0-0.5 | 只蹭简介的降权 |
| 广告卡 | 0 | 直接删 |

默认阈值 0.5。低于阈值删除。UP 主名不参与打分。

### 为什么不用分词库

- 搜索词通常 2-10 个字，bigram 足够。
- 避免把 jieba 之类的词典打进模块，减少体积和冲突。
- 纯 Kotlin，JVM 单测可覆盖。

## JSON 形态

三种已知响应：

1. `data.item[]` — APP 综合搜索
2. `data.result[]` — APP 分类搜索
3. `data.result[{result_type, data[]}]` — Web all/v2

识别规则：数组里存在元素同时具备 `title` 和 `bvid/param/goto/linktype` 之一，即视为结果数组。其余数组递归下钻。

不改 `numResults/numPages`，避免分页状态错乱；UI 显示的总数可能偏大，可接受。

## 本地重排（M3）

服务端顺序被个性化污染，仅过滤不够。保留项按以下 key 排序：

1. 相关性分数降序
2. 播放量降序（字段存在时）
3. 原始 position（稳定排序）

风险：B 站可能用 `position` 而非数组顺序渲染。M3 先做灰度开关，实测后再默认开启。

## 分页补偿（M5）

删完后本页可能只剩几条。用同一个 OkHttpClient 重放请求（`pn+1`），过滤后合并：

- 仅当保留数 < 5 且未到末页时触发
- 串行、限 1 页、请求间隔 ≥ 1s
- trackid 不匹配则放弃合并

## 兼容性

- 只在 `tv.danmaku.bili` 主进程加载；`:web`、`:download` 跳过。
- 只处理 HTTP 200、JSON、body < 1MB。
- 如果搜索改走 gRPC/protobuf，JSON 钩子自然失效（不会崩）；M6 增加 protobuf 分支。
- 重建响应时删除 `Content-Length` / `Content-Encoding`，防止解析器读到旧长度。

## 隐私

- 不上传任何数据。
- 不读取账号 Cookie。
- 日志只在本机 logcat，包含搜索词和被删标题（用于调试），release 可关闭。
