# Product

<!-- impeccable:product-schema 1 -->

<!-- Assumption notice: PRODUCT.md was inferred from the repository, the shipped 0.6.1 build, and the user's instruction "就默认顺序" (proceed without an interview). Every inferred field is marked below; no unverified market claims are recorded. -->

## Platform

android

## Users

Bilibili Android users who search often and are tired of irrelevant, ad-like, or off-topic results. They are comfortable installing an LSPosed/NPatch module and expect the official app to keep working. Their job: find a relevant video quickly without switching to a third-party client or losing their account context.

## Product Purpose

A local, on-device search-result filter and re-ranker for the official Bilibili Android app. It removes zero-relevance videos, ad/purchase cards, and user-defined noise, then optionally re-ranks video cards by text relevance and play count. Success means fewer junk results per search, with no extra network requests and no account actions.

## Positioning

An LSPosed module that hooks Bilibili's Moss gRPC search responses inside the official client. It is not a third-party client and not a browser userscript: it keeps the official app, filters the response before the UI renders it, and adds local rules and re-ranking that the server does not provide.

## Operating Context

- Android 9+ with LSPosed (Modern API 102) or NPatch.
- Bilibili Android 9.0+; search page, comprehensive and typed search tabs.
- A separate module app hosts settings and status; the hook runs inside the Bilibili process.
- Settings sync through LSPosed RemotePreferences; hook/search status returns through an explicit broadcast.
- Builds and unit tests run only in GitHub Actions; no local Gradle build is part of the workflow.

## Capabilities and Constraints

Confirmed capabilities:

- Hook `SearchAllResponse.getItemList` and `SearchByTypeResponse.getItemsList`.
- Remove `CM` / `PURCHASE` cards and `goto=ad` items.
- Relevance scoring from title/description/author/play, with a configurable threshold.
- Rules typed into the search box: plain terms, `-exclude`, `#tag` (falls back to description on the app), `@uploader`.
- Optional block words, blocked uploaders (name or mid), minimum play count, title dedupe, video-only, recommendation-card hiding, and local re-ranking.
- Material 3 settings screen with presets 30/50/70 and a 0-100 slider.
- Hook status and aggregate/last-search statistics in the settings screen.

Constraints:

- Search surface only; no home, player, dynamic, or account behavior.
- No extra network requests, no request rewriting, no signature bypass.
- The gRPC search card has no tag or publish date; real tag filtering and time sorting are unavailable.
- The bottom "related searches" block is a separate UI section and is not covered.
- Settings depend on LSPosed RemotePreferences; without the framework service the UI must not pretend settings are active.

## Brand Commitments

- Name: BiliSearch Fixer.
- Existing Bilibili-adjacent accent: pink `#FB7299`, with Material 3 DayNight surfaces.
- Voice: concise, technical Chinese; no hype, no fake benchmarks.
- Existing vector icon: magnifier plus play triangle.

## Evidence on Hand

- Real-device validation on OnePlus PLF110 / Android 16 / Bilibili 9.16.1.
- Logs: two hooks installed; `bad apple` kept 2-6 of 20 depending on settings; `Bad Dongxi [excluded:dongxi]`; `[min_play]` after setting the threshold.
- Settings-page screenshots for v0.5.x and v0.6.1.
- No fabricated user counts, testimonials, or performance claims.

## Product Principles

1. **Protect the result set, not the whole app.** Every change must be traceable to search relevance.
2. **Explain the filter, not the plumbing.** Users should see what is on, what it removed, and what it cannot do.
3. **Conservative defaults, explicit power.** Safe baseline; advanced rules and thresholds are opt-in.
4. **Trust through evidence.** Real counts from the last search beat vague "optimized" claims.
5. **Never break the host.** Prefer response-level filtering over UI surgery.

## Accessibility & Inclusion

- TalkBack labels on every control; switches use their visible text.
- Touch targets at least 48dp; Material 3 components.
- DayNight theme, no hardcoded light-only colors in new work.
- Chinese text must survive font scaling and long labels without clipping.
