package io.github.xiaoancute.bilisearch

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import java.util.Date

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var statusPrefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var thresholdLabel: TextView
    private lateinit var thresholdSlider: Slider
    private lateinit var presetGroup: MaterialButtonToggleGroup
    private var remoteAvailable = false
    private var syncingSlider = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val remote = BiliSearchApplication.remotePreferences()
        remoteAvailable = remote != null
        settingsPrefs = remote ?: getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        statusPrefs = getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE)

        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.subtitle = "v" + versionName() + " · " +
            if (remoteAvailable) "只改搜索" else "框架服务未连接"

        statusText = findViewById(R.id.statusText)
        statsText = findViewById(R.id.statsText)
        thresholdLabel = findViewById(R.id.thresholdLabel)
        thresholdSlider = findViewById(R.id.thresholdSlider)
        presetGroup = findViewById(R.id.presetGroup)

        bindSwitch(R.id.enableSwitch, KEY_ENABLED, true)
        bindSwitch(R.id.dropAdsSwitch, KEY_DROP_ADS, true)
        bindSwitch(R.id.videoOnlySwitch, KEY_VIDEO_ONLY, false)
        bindSwitch(R.id.hideRecommendSwitch, KEY_HIDE_RECOMMEND, false)
        bindSwitch(R.id.dedupeSwitch, KEY_DEDUPE, false)
        bindSwitch(R.id.rerankSwitch, KEY_RERANK, false)

        setupThreshold()
        setupRules()

        findViewById<MaterialButton>(R.id.refreshButton).setOnClickListener {
            refreshStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!remoteAvailable) {
            val remote = BiliSearchApplication.remotePreferences()
            if (remote != null) {
                remoteAvailable = true
                settingsPrefs = remote
                recreate()
                return
            }
        }
        refreshStatus()
    }

    private fun setupThreshold() {
        thresholdSlider.valueFrom = 0f
        thresholdSlider.valueTo = 100f
        thresholdSlider.stepSize = 5f

        val raw = settingsPrefs.getInt(KEY_THRESHOLD, 50).coerceIn(0, 100)
        val threshold = (Math.round(raw / 5f) * 5).coerceIn(0, 100)
        thresholdSlider.value = threshold.toFloat()
        thresholdLabel.text = "相关性阈值：" + threshold + "%"
        presetGroup.check(presetIdFor(threshold))

        thresholdSlider.addOnChangeListener(object : Slider.OnChangeListener {
            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                val progress = value.toInt()
                thresholdLabel.text = "相关性阈值：" + progress + "%"
                if (fromUser) {
                    editSettings { it.putInt(KEY_THRESHOLD, progress) }
                    syncingSlider = true
                    presetGroup.check(presetIdFor(progress))
                    syncingSlider = false
                }
            }
        })

        presetGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || syncingSlider) return@addOnButtonCheckedListener

            val value = when (checkedId) {
                R.id.presetLoose -> 30
                R.id.presetStandard -> 50
                R.id.presetStrict -> 70
                else -> return@addOnButtonCheckedListener
            }
            thresholdSlider.value = value.toFloat()
            editSettings { it.putInt(KEY_THRESHOLD, value) }
        }
    }

    private fun setupRules() {
        val minPlayInput = findViewById<TextInputEditText>(R.id.minPlayInput)
        val blockWordsInput = findViewById<TextInputEditText>(R.id.blockWordsInput)
        val blockUpsInput = findViewById<TextInputEditText>(R.id.blockUpsInput)
        val whitelistInput = findViewById<TextInputEditText>(R.id.whitelistInput)

        minPlayInput.setText(settingsPrefs.getInt(KEY_MIN_PLAY, 0).toString())
        blockWordsInput.setText(settingsPrefs.getString(KEY_BLOCK_WORDS, ""))
        blockUpsInput.setText(settingsPrefs.getString(KEY_BLOCK_UPS, ""))
        whitelistInput.setText(settingsPrefs.getString(KEY_WHITELIST, ""))

        findViewById<MaterialButton>(R.id.saveRulesButton).setOnClickListener {
            val minPlay = minPlayInput.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            editSettings {
                it.putInt(KEY_MIN_PLAY, minPlay)
                it.putString(KEY_BLOCK_WORDS, blockWordsInput.text?.toString().orEmpty())
                it.putString(KEY_BLOCK_UPS, blockUpsInput.text?.toString().orEmpty())
                it.putString(KEY_WHITELIST, whitelistInput.text?.toString().orEmpty())
            }
            val message = if (remoteAvailable) "规则已保存，下一次搜索生效" else "未连接框架服务，规则可能不生效"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSwitch(id: Int, key: String, defaultValue: Boolean) {
        val view = findViewById<MaterialSwitch>(id)
        view.isChecked = settingsPrefs.getBoolean(key, defaultValue)
        view.setOnCheckedChangeListener { _, checked ->
            editSettings { it.putBoolean(key, checked) }
        }
    }

    @Suppress("USELESS_ELVIS")
    private fun editSettings(block: (SharedPreferences.Editor) -> Unit) {
        val editor = settingsPrefs.edit() ?: return
        block(editor)
        editor.apply()
    }

    private fun presetIdFor(threshold: Int): Int = when {
        threshold <= 35 -> R.id.presetLoose
        threshold <= 65 -> R.id.presetStandard
        else -> R.id.presetStrict
    }

    private fun refreshStatus() {
        val hookTime = statusPrefs.getLong(KEY_STATUS_TIME, 0L)
        val installed = statusPrefs.getInt(KEY_STATUS_INSTALLED, 0)
        val process = statusPrefs.getString(KEY_STATUS_PROCESS, null)
        val version = statusPrefs.getString(KEY_STATUS_VERSION, null)

        val searchTime = statusPrefs.getLong(KEY_STATUS_SEARCH_TIME, 0L)
        val keyword = statusPrefs.getString(KEY_STATUS_SEARCH_KEYWORD, "") ?: ""
        val kept = statusPrefs.getInt(KEY_STATUS_SEARCH_KEPT, 0)
        val total = statusPrefs.getInt(KEY_STATUS_SEARCH_TOTAL, 0)
        val removed = statusPrefs.getInt(KEY_STATUS_SEARCH_REMOVED, 0)

        val hookText = when {
            hookTime > 0L -> {
                val whenText = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(hookTime))
                "Hook 已上报：" + whenText +
                    "\n安装 " + installed + " 个 Hook，进程 " + (process ?: "未知") +
                    "，模块 " + (version ?: "未知")
            }
            searchTime > 0L -> "Hook 已运行：最近一次搜索已上报，安装状态未回传。"
            else -> "尚未收到 Hook 上报。确认模块已启用、作用域已勾选 B 站，并重启 B 站。"
        }

        val recentText = if (searchTime == 0L) {
            "还没有搜索记录。"
        } else {
            val whenText = DateFormat.format("MM-dd HH:mm:ss", Date(searchTime))
            "最近：\"" + keyword + "\" 保留 " + kept + "/" + total +
                "，删除 " + removed + "（" + whenText + "）"
        }

        val totalSearches = statusPrefs.getInt(KEY_TOTAL_SEARCHES, 0)
        val totalRemoved = statusPrefs.getInt(KEY_TOTAL_REMOVED, 0)
        val warning = if (remoteAvailable) "" else "\n框架服务未连接，设置只能保存在本地。"

        statusText.text = hookText + warning
        statsText.text = "累计：搜索 " + totalSearches + " 次，删除 " + totalRemoved + " 条\n" + recentText
    }

    private fun versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "?"
}
