# Xposed entry points must never be renamed or removed.
-keep class io.github.xiaoancute.bilisearch.XposedInit { *; }
-keepclassmembers class * {
    @de.robv.android.xposed.* <methods>;
}
