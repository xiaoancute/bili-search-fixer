# BiliSearch Fixer

> LSPosed 模块：把 B 站客户端搜索结果里「和关键词毫无关系」的内容删掉。

![build](https://github.com/xiaoancute/bili-search-fixer/actions/workflows/build.yml/badge.svg)

## 问题

B 站搜索的相关性已经烂到众所周知。搜索结果里混着广告卡、买量视频、个性化推荐、以及和关键词 100% 零关联的内容；时间排序甚至会把更新的视频排到后面。游客/无痕模式连排序和筛选入口都不给。

详见 [docs/RESEARCH.md](docs/RESEARCH.md)，里面摘了 NGA、V2EX 上 2021-2026 年的吐槽原帖。

## 方案

不去逆向搜索 UI，而是在 **OkHttp 响应层**做 JSON 重写：

```
搜索请求 -> OkHttpClient.newCall() [Hook]
          -> 响应 URL 命中 /x/v2/search
          -> 读取 body，递归找出结果数组
          -> 按关键词做文本相关性打分，删掉 0 关联项和广告
          -> 重建 ResponseBody，B 站解析器无感知
```

这样做的好处：不依赖混淆类名、不改 RecyclerView、覆盖综合/分类/翻页所有入口。

## 当前状态

| 能力 | 状态 |
|---|---|
| Xposed 入口 + 进程过滤 | ✅ |
| OkHttp newCall Hook + 搜索 URL 识别 | ✅ |
| 关键词归一化 / 中文 bigram / 相关性打分 | ✅ 有单测 |
| 搜索 JSON 递归重写器 | ✅ 有单测（待接线） |
| 响应体替换（真正删结果） | ⏳ M2 |
| 设置页 / 白名单 / 本地重排 | ⏳ M3+ |

v0.1.0 是「能装、能打日志、算法可验证」的地基，不会改搜索结果。

## 构建（只用 GitHub Actions）

本仓库**不在本地编译**。推送到 `main` 后 Action 会自动跑单测并产出 APK。

```bash
# 下载最近一次构建产物
gh run list --workflow build.yml --limit 1
gh run download --name BiliSearchFixer-debug
```

产物：`app-debug.apk`（debug 签名，LSPosed 不校验签名）。

## 安装

1. 手机安装 LSPosed（Zygisk）。
2. 安装 APK，在 LSPosed 里启用模块。
3. 作用域勾选「哔哩哔哩」(`tv.danmaku.bili`)。
4. 强制停止并重启 B 站。
5. 看日志：

```bash
adb logcat | grep BiliSearchFixer
```

## 路线图

- [x] M1 工程骨架 + CI + Hook 打点
- [ ] M2 响应体替换，真正删除 0 关联项和广告卡
- [ ] M3 本地重排（相关性 > 播放量），对抗个性化排序
- [ ] M4 设置页：强度、关键词/UP 白名单、关键词排除
- [ ] M5 分页补偿 + 标题去重（搬运/切片）
- [ ] M6 gRPC/protobuf 搜索链路兜底

## 参考

- [BiliRoamingX](https://github.com/zjns/BiliRoamingX) — Tribe ClassLoader、DexKit、OkHttp/ProtoBuf 改写思路（GPL-3.0，仅参考，不复制代码）
- [DexKit](https://github.com/LuckyPray/DexKit) — 运行时定位混淆类
- [bilibili-cleaner](https://github.com/festoney8/bilibili-cleaner) — 网页端过滤规则模型
- [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) — 搜索接口字段

## 免责声明

仅供个人学习与本地使用。模块只读取响应，不向任何第三方服务器发送数据，不修改账号状态。与哔哩哔哩官方无关。

## License

GPL-3.0
