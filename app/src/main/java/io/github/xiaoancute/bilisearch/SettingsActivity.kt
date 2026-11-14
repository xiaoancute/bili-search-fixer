package io.github.xiaoancute.bilisearch

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Date

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var statusPrefs: SharedPreferences
    private lateinit var toolbar: MaterialToolbar
    private lateinit var scrollView: NestedScrollView
    private lateinit var filterContent: View
    private lateinit var rulesContent: View
    private lateinit var statusContent: View
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var thresholdLabel: TextView
    private lateinit var thresholdSlider: Slider
    private lateinit var presetGroup: MaterialButtonToggleGroup
    private var remoteAvailable = false
    private var syncingSlider = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val remote = BiliSearchApplication.remotePreferences()
        remoteAvailable = remote != null
        settingsPrefs = remote ?: getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        statusPrefs = getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE)

        setContentView(R.layout.activity_settings)
        applyInsets()

        toolbar = findViewById(R.id.toolbar)
        scrollView = findViewById(R.id.scroll)
        filterContent = findViewById(R.id.filterTabContent)
        rulesContent = findViewById(R.id.rulesTabContent)
        statusContent = findViewById(R.id.statusTabContent)
        toolbar.subtitle = "BiliSearch Fixer · v" + versionName()

        statusText = findViewById(R.id.statusText)
        statsText = findViewById(R.id.statsText)
        thresholdLabel = findViewById(R.id.thresholdLabel)
        thresholdSlider = findViewById(R.id.thresholdSlider)
        presetGroup = findViewById(R.id.presetGroup)

        findViewById<TextView>(R.id.syncWarning).visibility =
            if (remoteAvailable) View.GONE else View.VISIBLE

        bindSwitch(R.id.enableSwitch, KEY_ENABLED, true)
        bindSwitch(R.id.dropAdsSwitch, KEY_DROP_ADS, true)
        bindSwitch(R.id.videoOnlySwitch, KEY_VIDEO_ONLY, false)
        bindSwitch(R.id.hideRecommendSwitch, KEY_HIDE_RECOMMEND, false)
        bindSwitch(R.id.dedupeSwitch, KEY_DEDUPE, false)
        bindSwitch(R.id.rerankSwitch, KEY_RERANK, false)

        setupThreshold()
        setupRules()

        if (!remoteAvailable) {
            setControlsEnabled(false)
        }

        findViewById<MaterialButton>(R.id.refreshButton).setOnClickListener {
            refreshStatus()
        }

        setupTabs()
    }

    private fun setupTabs() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        bottomNav.selectedItemId = R.id.tabFilter
        showTab(R.id.tabFilter)
    }

    private fun showTab(tabId: Int) {
        filterContent.visibility = if (tabId == R.id.tabFilter) View.VISIBLE else View.GONE
        rulesContent.visibility = if (tabId == R.id.tabRules) View.VISIBLE else View.GONE
        statusContent.visibility = if (tabId == R.id.tabStatus) View.VISIBLE else View.GONE

        toolbar.title = when (tabId) {
            R.id.tabFilter -> "过滤"
            R.id.tabRules -> "规则"
            R.id.tabStatus -> "状态"
            else -> getString(R.string.app_name)
        }

        currentFocus?.clearFocus()
        scrollView.scrollTo(0, 0)
        if (tabId == R.id.tabStatus) refreshStatus()
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

    override fun onPause() {
        super.onPause()
        if (remoteAvailable) saveRules(showError = false)
    }

    private fun applyInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun setControlsEnabled(enabled: Boolean) {
        intArrayOf(
            R.id.presetGroup,
            R.id.thresholdSlider,
            R.id.enableSwitch,
            R.id.dropAdsSwitch,
            R.id.videoOnlySwitch,
            R.id.hideRecommendSwitch,
            R.id.dedupeSwitch,
            R.id.rerankSwitch,
            R.id.minPlayLayout,
            R.id.minPlayInput,
            R.id.blockWordsInput,
            R.id.blockUpsInput,
            R.id.whitelistInput
        ).forEach { findViewById<View>(it).isEnabled = enabled }

        intArrayOf(R.id.presetLoose, R.id.presetStandard, R.id.presetStrict)
            .forEach { findViewById<View>(it).isEnabled = enabled }
    }

    private fun setupThreshold() {
        thresholdSlider.valueFrom = 0f
        thresholdSlider.valueTo = 100f
        thresholdSlider.stepSize = 5f

        val raw = settingsPrefs.getInt(KEY_THRESHOLD, 50).coerceIn(0, 100)
        val threshold = (Math.round(raw / 5f) * 5).coerceIn(0, 100)
        thresholdSlider.value = threshold.toFloat()
        thresholdLabel.text = "相关性阈值：" + threshold + "%"
        updateThresholdA11y(threshold)
        presetGroup.check(presetIdFor(threshold))

        thresholdSlider.addOnChangeListener(object : Slider.OnChangeListener {
            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                val progress = value.toInt()
                thresholdLabel.text = "相关性阈值：" + progress + "%"
                updateThresholdA11y(progress)
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

    private fun updateThresholdA11y(progress: Int) {
        thresholdSlider.contentDescription = "相关性阈值 " + progress + "%"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            thresholdSlider.stateDescription = progress.toString() + "%"
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

        listOf(minPlayInput, blockWordsInput, blockUpsInput, whitelistInput).forEach { input ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) saveRules(showError = true)
            }
        }
    }

    private fun saveRules(showError: Boolean) {
        if (!::settingsPrefs.isInitialized || !remoteAvailable) return

        val minPlayLayout = findViewById<TextInputLayout>(R.id.minPlayLayout)
        val raw = findViewById<TextInputEditText>(R.id.minPlayInput)
            .text?.toString()?.trim().orEmpty()
        val parsed = raw.toIntOrNull()
        val minPlay = when {
            raw.isEmpty() -> 0
            parsed != null && parsed >= 0 -> parsed
            else -> null
        }

        if (minPlay == null) {
            if (showError) minPlayLayout.error = "请输入 0 或正整数"
        } else {
            minPlayLayout.error = null
        }

        val blockWords = findViewById<TextInputEditText>(R.id.blockWordsInput)
            .text?.toString().orEmpty()
        val blockUps = findViewById<TextInputEditText>(R.id.blockUpsInput)
            .text?.toString().orEmpty()
        val whitelist = findViewById<TextInputEditText>(R.id.whitelistInput)
            .text?.toString().orEmpty()

        editSettings {
            if (minPlay != null) it.putInt(KEY_MIN_PLAY, minPlay)
            it.putString(KEY_BLOCK_WORDS, blockWords)
            it.putString(KEY_BLOCK_UPS, blockUps)
            it.putString(KEY_WHITELIST, whitelist)
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
        if (!remoteAvailable) return
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
        val searchTime = statusPrefs.getLong(KEY_STATUS_SEARCH_TIME, 0L)
        val keyword = statusPrefs.getString(KEY_STATUS_SEARCH_KEYWORD, "") ?: ""
        val kept = statusPrefs.getInt(KEY_STATUS_SEARCH_KEPT, 0)
        val total = statusPrefs.getInt(KEY_STATUS_SEARCH_TOTAL, 0)
        val removed = statusPrefs.getInt(KEY_STATUS_SEARCH_REMOVED, 0)
        val totalSearches = statusPrefs.getInt(KEY_TOTAL_SEARCHES, 0)
        val totalRemoved = statusPrefs.getInt(KEY_TOTAL_REMOVED, 0)

        val title = when {
            hookTime > 0L || searchTime > 0L -> "搜索过滤已生效"
            remoteAvailable -> "已连接，等待第一次搜索"
            else -> "未连接 LSPosed，设置不会同步"
        }

        val detail = when {
            hookTime > 0L -> {
                val whenText = DateFormat.format("MM-dd HH:mm", Date(hookTime))
                "上次上报 " + whenText + " · 安装 " + installed + " 个 Hook"
            }
            searchTime > 0L -> "最近一次搜索已上报"
            remoteAvailable -> "打开 B 站搜索一次即可确认"
            else -> "在 LSPosed 中启用模块并勾选 B 站"
        }

        val recentText = if (searchTime == 0L) {
            "还没有搜索记录。"
        } else {
            val whenText = DateFormat.format("MM-dd HH:mm", Date(searchTime))
            "最近：" + keyword + " · 保留 " + kept + "/" + total +
                " · 删除 " + removed + "（" + whenText + "）"
        }

        statusText.text = title + "\n" + detail
        statsText.text = "累计：搜索 " + totalSearches + " 次，删除 " + totalRemoved + " 条\n" + recentText
    }

    private fun versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "?"
}
