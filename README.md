# BiliSearch Fixer

> LSPosed 模块：在 B 站官方客户端里过滤搜索结果的无关视频、广告卡和买量卡。只改搜索内容。

![build](https://github.com/xiaoancute/bili-search-fixer/actions/workflows/build.yml/badge.svg)

当前版本：**1.0.0**

## 功能

| 能力 | 说明 |
|---|---|
| 搜索过滤 | 综合搜索 + 分类搜索（视频 / 用户 / 直播等 Tab） |
| 广告清理 | `CM` / `PURCHASE` / `goto=ad` 直接删除 |
| 相关性过滤 | 按标题、简介、UP 主、播放量打分，删除零关联结果 |
| 搜索框规则 | `-排除词` / `#标签` / `@UP主` |
| 结果整理 | 最低播放量、屏蔽词、屏蔽 UP、白名单、标题去重、只保留视频、隐藏推荐卡、本地重排 |
| 过滤强度 | 宽松 / 标准 / 严格三档 |
| 运行状态 | Hook 是否生效、累计与最近一次搜索的删除数 |

只 Hook 搜索响应，不改宿主 UI、不改请求、不发额外网络请求。

## 规则语法

搜索词里可以直接带：

```
摄影 -入门 #ai @影视飓风
```

| 语法 | 行为 |
|---|---|
| `词` | 标题必须包含 |
| `-词` | 标题包含即删 |
| `#词` | 标签必须包含；App 响应没有 tag，回退到简介 |
| `@词` | 作者匹配任一 |

无修饰词时只跑自动相关性；出现任一修饰词后进入严格模式，普通词必须全部出现在标题。

## 安装

1. Android 9+，安装 LSPosed（Modern API 102）或 NPatch。
2. 安装 APK。
3. 在框架里启用模块，作用域勾选「哔哩哔哩」(`tv.danmaku.bili`)。
4. 强制停止并重启 B 站，搜索一次。
5. 打开模块 App 的「状态」页，显示「搜索过滤已生效」即正常。

日志：

```bash
adb logcat | grep BiliSearchFixer
```

## 已知限制

- 搜索卡片没有 tag 字段，`#` 规则只能匹配简介。
- 搜索卡片没有发布日期，无法按时间排序。
- 底部「相关搜索」是独立区块，当前不覆盖。
- 搜索词仍原样发给 B 站，`-排除词` 只在本地生效。
- 仅适配 B 站 9.0+；实测 9.16.1。

## 构建与下载

构建、单测和 APK 全部在 GitHub Actions 完成，不在本地编译。

```bash
gh run list --workflow build.yml --limit 1
gh run download --name BiliSearchFixer-release   # 日常安装
gh run download --name BiliSearchFixer-debug     # 调试
```

## 免责声明

仅供个人学习与本地使用。模块只读取搜索响应，不向第三方服务器发送数据，不修改账号状态。与哔哩哔哩官方无关。

License: GPL-3.0
