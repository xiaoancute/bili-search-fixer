package io.github.xiaoancute.bilisearch

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateFormat
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Date
import io.github.xiaoancute.bilisearch.filter.SearchDiagnostics
import io.github.xiaoancute.bilisearch.filter.SearchRulePreview

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
    private lateinit var removalSummaryText: TextView
    private lateinit var removalSamplesText: TextView
    private lateinit var strengthHelp: TextView
    private lateinit var presetGroup: MaterialButtonToggleGroup
    private var remoteAvailable = false
    private var strengthReady = false
    private var selectedTab = R.id.tabFilter

    private val serviceListener = BiliSearchApplication.ServiceStateListener { service ->
        runOnUiThread {
            if (!isFinishing) {
                if (service == null) {
                    remoteAvailable = false
                    findViewById<TextView>(R.id.syncWarning)?.visibility = View.VISIBLE
                    if (::settingsStore.isInitialized) setControlsEnabled(false)
                    if (::statusStore.isInitialized) refreshStatus()
                    updateRulePreview()
                } else if (!remoteAvailable) {
                    recreate()
                }
            }
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
        removalSummaryText = findViewById(R.id.removalSummaryText)
        removalSamplesText = findViewById(R.id.removalSamplesText)
        strengthHelp = findViewById(R.id.strengthHelp)
        presetGroup = findViewById(R.id.presetGroup)

        findViewById<TextView>(R.id.syncWarning).visibility =
            if (remoteAvailable) View.GONE else View.VISIBLE

        bindSwitch(R.id.enableSwitch, { it.enabled }, { s, v -> s.copy(enabled = v) })
        bindSwitch(R.id.dropAdsSwitch, { it.dropAds }, { s, v -> s.copy(dropAds = v) })
        bindSwitch(R.id.videoOnlySwitch, { it.videoOnly }, { s, v -> s.copy(videoOnly = v) })
        bindSwitch(R.id.hideRecommendSwitch, { it.hideRecommend }, { s, v -> s.copy(hideRecommend = v) })
        bindSwitch(R.id.dedupeSwitch, { it.dedupe }, { s, v -> s.copy(dedupe = v) })
        bindSwitch(R.id.rerankSwitch, { it.rerank }, { s, v -> s.copy(rerank = v) })

        setupStrength()
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
                // Let the recreated instance read the remote store. Mutating
                // remoteAvailable here would make onPause write the local
                // fallback values back over the remote ones.
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
            R.id.whitelistInput,
            R.id.rulePreviewKeywordInput,
            R.id.rulePreviewTitleInput,
            R.id.rulePreviewUploaderInput
        ).forEach { findViewById<View>(it).isEnabled = enabled }

        intArrayOf(R.id.presetLoose, R.id.presetStandard, R.id.presetStrict)
            .forEach { findViewById<View>(it).isEnabled = enabled }
    }

    private fun setupStrength() {
        strengthReady = false
        presetGroup.isSaveEnabled = false
        intArrayOf(R.id.presetLoose, R.id.presetStandard, R.id.presetStrict).forEach { id ->
            val button = findViewById<View>(id)
            button.isSaveEnabled = false
            button.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) strengthReady = true
                false
            }
            button.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) strengthReady = true
            }
        }
        bindStrength(FilterStrength.fromThreshold(settings.threshold))

        presetGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || !strengthReady) return@addOnButtonCheckedListener

            val strength = when (checkedId) {
                R.id.presetLoose -> FilterStrength.LOOSE
                R.id.presetStandard -> FilterStrength.STANDARD
                R.id.presetStrict -> FilterStrength.STRICT
                else -> return@addOnButtonCheckedListener
            }
            updateSettings { it.copy(threshold = strength.threshold) }
            bindStrength(strength)
        }
    }

    private fun bindStrength(strength: FilterStrength) {
        presetGroup.check(presetIdFor(strength))
        strengthHelp.text = when (strength) {
            FilterStrength.LOOSE -> "宽松：保留部分关键词匹配，也允许主要在简介中匹配；可能留下边缘内容。"
            FilterStrength.STANDARD -> "标准（推荐）：标题需覆盖较多关键词；仅简介匹配的结果会删。"
            FilterStrength.STRICT -> "严格：标题需覆盖更多关键词；高亮只作加分，适合精确查找，容易删多。"
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

        val previewInputs = listOf(
            findViewById<TextInputEditText>(R.id.rulePreviewKeywordInput),
            findViewById<TextInputEditText>(R.id.rulePreviewTitleInput),
            findViewById<TextInputEditText>(R.id.rulePreviewUploaderInput)
        )
        previewInputs.forEach { input ->
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateRulePreview()
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        updateRulePreview()
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
        updateRulePreview()
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

    private fun updateRulePreview() {
        if (!::settings.isInitialized) return

        val keyword = findViewById<TextInputEditText>(R.id.rulePreviewKeywordInput).text?.toString().orEmpty()
        val title = findViewById<TextInputEditText>(R.id.rulePreviewTitleInput).text?.toString().orEmpty()
        val uploader = findViewById<TextInputEditText>(R.id.rulePreviewUploaderInput).text?.toString().orEmpty()
        val preview = SearchRulePreview.evaluate(keyword, title, uploader, settings)
        val lines = ArrayList<String>()

        if (!remoteAvailable) {
            lines += "LSPosed 设置未连接，无法读取当前已保存规则。连接后可试算。"
        } else {
            if (keyword.isBlank()) {
                lines += "填写搜索词，可试算白名单和屏蔽词与查询的冲突。"
            } else if (preview.whitelistMatch != null) {
                lines += "白名单命中「${preview.whitelistMatch}」：这次搜索会跳过所有本地过滤。"
            } else {
                lines += "白名单：未命中。"
            }

            if (title.isBlank()) {
                lines += "填写视频标题，可试算屏蔽词。"
            } else if (preview.whitelistMatch != null) {
                lines += "白名单生效，屏蔽词不会删除这次搜索的结果。"
            } else if (preview.blockedWordMatch != null) {
                lines += "屏蔽词命中标题「${preview.blockedWordMatch}」：视频会被删除。"
            } else {
                lines += "屏蔽词：标题未命中。"
            }

            if (uploader.isBlank()) {
                lines += "填写 UP 主名称或 mid，可试算屏蔽 UP。"
            } else if (preview.whitelistMatch != null) {
                lines += "白名单生效，屏蔽 UP 不会删除这次搜索的结果。"
            } else if (preview.blockedUploaderMatch != null) {
                lines += "屏蔽 UP 命中「${preview.blockedUploaderMatch}」：视频会被删除。"
            } else {
                lines += "屏蔽 UP：未命中。"
            }

            if (preview.whitelistMatch == null && preview.queryConflicts.isNotEmpty()) {
                lines += "注意：搜索词包含屏蔽词「${preview.queryConflicts.joinToString("、")}」；标题包含这些词的视频会被删除。"
            }

            if (keyword.isNotBlank() && title.isNotBlank() && uploader.isNotBlank() &&
                preview.whitelistMatch == null && preview.blockedWordMatch == null && preview.blockedUploaderMatch == null
            ) {
                lines += "此示例未命中白名单、屏蔽词或屏蔽 UP。"
            }
        }

        if (remoteAvailable && preview.invalidBlockWords.isNotEmpty()) {
            lines += "无效屏蔽词（只有标点或空白）：${preview.invalidBlockWords.joinToString("、")}；不会匹配结果。"
        }
        if (remoteAvailable && preview.invalidBlockUps.isNotEmpty()) {
            lines += "无效屏蔽 UP（只有标点或空白）：${preview.invalidBlockUps.joinToString("、")}；不会匹配结果。"
        }
        if (remoteAvailable && preview.invalidWhitelist.isNotEmpty()) {
            lines += "无效白名单（只有标点或空白）：${preview.invalidWhitelist.joinToString("、")}；不会跳过过滤。"
        }

        findViewById<TextView>(R.id.rulePreviewResult).text = lines.joinToString("\n")
    }

    private fun presetIdFor(strength: FilterStrength): Int = when (strength) {
        FilterStrength.LOOSE -> R.id.presetLoose
        FilterStrength.STANDARD -> R.id.presetStandard
        FilterStrength.STRICT -> R.id.presetStrict
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
            "最近一批：" + status.keyword + " · 保留 " + status.kept + "/" + status.total +
                " · 删除 " + status.removed + "（" + whenText + "）"
        }

        statusText.text = title + "\n" + detail
        statsText.text = "累计：处理 " + status.totalSearches + " 批结果，删除 " +
            status.totalRemoved + " 条\n" + recentText

        val diagnostics = status.diagnostics
        removalSummaryText.text = when {
            !status.hasSearch -> "搜索一次后，这里会展示最近一批结果的删除原因。"
            diagnostics == null -> "这条记录暂无删除原因；再次搜索后更新。"
            status.removed == 0 -> "这批结果没有删除。"
            else -> SearchDiagnosticsText.summary(diagnostics)
        }
        removalSamplesText.text = when {
            !status.hasSearch || diagnostics == null || status.removed == 0 -> ""
            diagnostics.samples.isEmpty() -> "本次删除的卡片没有可展示的视频标题。"
            else -> SearchDiagnosticsText.samples(diagnostics)
        }
        removalSamplesText.visibility = if (removalSamplesText.text.isEmpty()) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.removalSamplesHelp).apply {
            text = "最多展示 ${SearchDiagnostics.MAX_SAMPLES} 条被删视频，方便判断是否需要调低强度或修改规则。"
            visibility = if (diagnostics?.samples?.isNotEmpty() == true) View.VISIBLE else View.GONE
        }
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
