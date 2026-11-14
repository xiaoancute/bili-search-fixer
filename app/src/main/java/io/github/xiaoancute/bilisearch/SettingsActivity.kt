package io.github.xiaoancute.bilisearch

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import java.util.Date

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusLabel: TextView
    private lateinit var thresholdLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        window.statusBarColor = Color.rgb(20, 24, 38)
        title = "BiliSearch Fixer"
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(32))
            setBackgroundColor(Color.rgb(247, 247, 250))
        }

        root.addView(text("BiliSearch Fixer", 26f, Color.rgb(20, 24, 38), true))
        root.addView(
            text("只改 B 站搜索内容", 14f, Color.DKGRAY, false).apply {
                setPadding(0, dp(4), 0, dp(18))
            }
        )

        statusLabel = text("", 13f, Color.DKGRAY, false).apply {
            setPadding(0, 0, 0, dp(18))
        }
        root.addView(statusLabel)

        root.addView(
            Switch(this).apply {
                text = "启用搜索过滤"
                isChecked = prefs.getBoolean(KEY_ENABLED, true)
                setOnCheckedChangeListener { _, value ->
                    prefs.edit().putBoolean(KEY_ENABLED, value).apply()
                }
            }
        )

        root.addView(
            Switch(this).apply {
                text = "删除广告 / 商品卡"
                isChecked = prefs.getBoolean(KEY_DROP_ADS, true)
                setOnCheckedChangeListener { _, value ->
                    prefs.edit().putBoolean(KEY_DROP_ADS, value).apply()
                }
            }
        )

        thresholdLabel = text("", 15f, Color.rgb(20, 24, 38), true).apply {
            setPadding(0, dp(20), 0, dp(4))
        }
        root.addView(thresholdLabel)

        val seek = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt(KEY_THRESHOLD, 50)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    thresholdLabel.text = "相关性阈值：" + progress + "%"
                    if (fromUser) {
                        prefs.edit().putInt(KEY_THRESHOLD, progress).apply()
                    }
                }

                override fun onStartTrackingTouch(bar: SeekBar?) = Unit

                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        thresholdLabel.text = "相关性阈值：" + seek.progress + "%"
        root.addView(seek)

        root.addView(
            text("越高越严格。默认 50%，只删和关键词零关联的结果。", 12f, Color.GRAY, false).apply {
                setPadding(0, dp(4), 0, dp(22))
            }
        )

        root.addView(text("规则语法", 16f, Color.rgb(20, 24, 38), true))
        root.addView(
            text(
                "摄影 -入门 #ai @影视飓风\n\n" +
                    "- 排除词    # 标签/简介    @ UP主\n" +
                    "搜索时直接输入，模块会在本地过滤结果。",
                13f,
                Color.DKGRAY,
                false
            ).apply { setPadding(0, dp(4), 0, dp(8)) }
        )

        root.addView(
            Button(this).apply {
                text = "刷新 Hook 状态"
                setOnClickListener { refreshStatus() }
            }.apply { setPadding(0, dp(18), 0, 0) }
        )

        root.addView(
            text(
                "使用步骤：LSPosed / NPatch 启用模块 → 作用域勾选「哔哩哔哩」" +
                    "→ 强制停止并重启 B 站 → 搜索一次。",
                12f,
                Color.GRAY,
                false
            ).apply { setPadding(0, dp(18), 0, 0) }
        )

        return ScrollView(this).apply { addView(root) }
    }

    private fun refreshStatus() {
        val time = prefs.getLong(KEY_STATUS_TIME, 0L)
        val installed = prefs.getInt(KEY_STATUS_INSTALLED, 0)
        val process = prefs.getString(KEY_STATUS_PROCESS, null)
        val version = prefs.getString(KEY_STATUS_VERSION, null)

        val searchTime = prefs.getLong(KEY_STATUS_SEARCH_TIME, 0L)
        val searchKeyword = prefs.getString(KEY_STATUS_SEARCH_KEYWORD, null)
        val kept = prefs.getInt(KEY_STATUS_SEARCH_KEPT, 0)
        val total = prefs.getInt(KEY_STATUS_SEARCH_TOTAL, 0)
        val removed = prefs.getInt(KEY_STATUS_SEARCH_REMOVED, 0)

        val hookText = if (time == 0L) {
            "尚未收到 Hook 上报。确认模块已启用、作用域已勾选 B 站，并重启 B 站。"
        } else {
            val whenText = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(time))
            "Hook 已上报：" + whenText +
                "\n安装 " + installed + " 个 Hook，进程 " + (process ?: "未知") +
                "，模块 " + (version ?: "未知")
        }

        val searchText = if (searchTime == 0L) {
            "尚未记录到搜索过滤。重启 B 站后搜索一次。"
        } else {
            val whenText = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(searchTime))
            "最近搜索：\"" + (searchKeyword ?: "") + "\"" +
                "\n保留 " + kept + "/" + total + "，删除 " + removed +
                "（" + whenText + "）"
        }

        statusLabel.text = hookText + "\n\n" + searchText
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
