package io.github.xiaoancute.bilisearch

import android.content.Context
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

    private lateinit var settingsStore: SettingsStore
    private lateinit var statusStore: StatusStore
    private lateinit var settings: Settings
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
    private var selectedTab = R.id.tabFilter

    private val serviceListener = BiliSearchApplication.ServiceStateListener { service ->
        if (service != null && !remoteAvailable) {
            runOnUiThread { if (!isFinishing) recreate() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val remote = BiliSearchApplication.remotePreferences()
        remoteAvailable = remote != null
        settingsStore = SettingsStore(remote ?: getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE))
        statusStore = StatusStore(getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE))
        settings = settingsStore.read()

        setContentView(R.layout.activity_settings)
        applyInsets()
        selectedTab = savedInstanceState?.getInt(STATE_TAB, R.id.tabFilter) ?: R.id.tabFilter

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

        bindSwitch(R.id.enableSwitch, { it.enabled }, { s, v -> s.copy(enabled = v) })
        bindSwitch(R.id.dropAdsSwitch, { it.dropAds }, { s, v -> s.copy(dropAds = v) })
        bindSwitch(R.id.videoOnlySwitch, { it.videoOnly }, { s, v -> s.copy(videoOnly = v) })
        bindSwitch(R.id.hideRecommendSwitch, { it.hideRecommend }, { s, v -> s.copy(hideRecommend = v) })
        bindSwitch(R.id.dedupeSwitch, { it.dedupe }, { s, v -> s.copy(dedupe = v) })
        bindSwitch(R.id.rerankSwitch, { it.rerank }, { s, v -> s.copy(rerank = v) })

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
        bottomNav.isSaveEnabled = false
        bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        bottomNav.selectedItemId = selectedTab
        showTab(selectedTab)
    }

    private fun showTab(tabId: Int) {
        selectedTab = tabId
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

    override fun onStart() {
        super.onStart()
        BiliSearchApplication.addServiceStateListener(serviceListener)
    }

    override fun onStop() {
        BiliSearchApplication.removeServiceStateListener(serviceListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!remoteAvailable) {
            val remote = BiliSearchApplication.remotePreferences()
            if (remote != null) {
                remoteAvailable = true
                settingsStore = SettingsStore(remote)
                recreate()
                return
            }
        }
        refreshStatus()
    }

    override fun onPause() {
        super.onPause()
        saveRules(showError = false)
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

        val threshold = (Math.round(settings.threshold.coerceIn(0, 100) / 5f) * 5).coerceIn(0, 100)
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
                    updateSettings { it.copy(threshold = progress) }
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
            updateSettings { it.copy(threshold = value) }
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

        minPlayInput.setText(settings.minPlay.toString())
        blockWordsInput.setText(settings.blockWords.joinToString("\n"))
        blockUpsInput.setText(settings.blockUps.joinToString("\n"))
        whitelistInput.setText(settings.whitelist.joinToString("\n"))

        listOf(minPlayInput, blockWordsInput, blockUpsInput, whitelistInput).forEach { input ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) saveRules(showError = true)
            }
        }
    }

    private fun saveRules(showError: Boolean) {
        if (!::settingsStore.isInitialized || !remoteAvailable) return

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

        val next = settings.copy(
            minPlay = minPlay ?: settings.minPlay,
            blockWords = SettingsStore.parseList(
                findViewById<TextInputEditText>(R.id.blockWordsInput).text?.toString().orEmpty()
            ),
            blockUps = SettingsStore.parseList(
                findViewById<TextInputEditText>(R.id.blockUpsInput).text?.toString().orEmpty()
            ),
            whitelist = SettingsStore.parseList(
                findViewById<TextInputEditText>(R.id.whitelistInput).text?.toString().orEmpty()
            )
        )
        updateSettings { next }
    }

    private fun bindSwitch(
        id: Int,
        read: (Settings) -> Boolean,
        update: (Settings, Boolean) -> Settings
    ) {
        val view = findViewById<MaterialSwitch>(id)
        view.isChecked = read(settings)
        view.setOnCheckedChangeListener { _, checked ->
            updateSettings { update(it, checked) }
        }
    }

    private fun updateSettings(transform: (Settings) -> Settings) {
        if (!remoteAvailable) return
        settings = transform(settings)
        settingsStore.write(settings)
    }

    private fun presetIdFor(threshold: Int): Int = when {
        threshold <= 35 -> R.id.presetLoose
        threshold <= 65 -> R.id.presetStandard
        else -> R.id.presetStrict
    }

    private fun refreshStatus() {
        val status = statusStore.read()

        val title = when {
            status.hasHook || status.hasSearch -> "搜索过滤已生效"
            remoteAvailable -> "已连接，等待第一次搜索"
            else -> "未连接 LSPosed，设置不会同步"
        }

        val detail = when {
            status.hasHook -> {
                val whenText = DateFormat.format("MM-dd HH:mm", Date(status.hookTime))
                "上次上报 " + whenText + " · 安装 " + status.installed + " 个 Hook"
            }
            status.hasSearch -> "最近一次搜索已上报"
            remoteAvailable -> "打开 B 站搜索一次即可确认"
            else -> "在 LSPosed 中启用模块并勾选 B 站"
        }

        val recentText = if (!status.hasSearch) {
            "还没有搜索记录。"
        } else {
            val whenText = DateFormat.format("MM-dd HH:mm", Date(status.searchTime))
            "最近：" + status.keyword + " · 保留 " + status.kept + "/" + status.total +
                " · 删除 " + status.removed + "（" + whenText + "）"
        }

        statusText.text = title + "\n" + detail
        statsText.text = "累计：搜索 " + status.totalSearches + " 次，删除 " +
            status.totalRemoved + " 条\n" + recentText
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    private fun versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "?"

    private companion object {
        const val STATE_TAB = "selected_tab"
    }
}
