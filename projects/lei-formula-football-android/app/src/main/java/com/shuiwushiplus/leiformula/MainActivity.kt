package com.shuiwushiplus.leiformula

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var homeInput: EditText
    private lateinit var awayInput: EditText
    private lateinit var modeSpinner: Spinner
    private lateinit var summaryOutput: TextView
    private lateinit var output: TextView
    private lateinit var historyText: TextView
    private lateinit var homeScoreInput: EditText
    private lateinit var awayScoreInput: EditText
    private var lastPrediction: Prediction? = null
    private var lastRecordIndex: Int = -1
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val prefs by lazy { getSharedPreferences("lei_formula_history", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "雷式足球盲测"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(32))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "雷公式 · 双维随机盲测"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "正时背景盘 + 完全随机盘 + 八刻微盘 + 主客定位对照\n随机时间范围：1900-01-01 至 2099-12-31（UTC+8）"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(14))
        })

        homeInput = edit("主队名称（可留空）")
        awayInput = edit("客队名称（可留空）")
        root.addView(homeInput)
        root.addView(awayInput)

        root.addView(TextView(this).apply { text = "主客定位模型"; textSize = 15f; setPadding(0, dp(10), 0, dp(4)) })
        modeSpinner = Spinner(this)
        val modes = PositionMode.values().map { it.label }
        modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        modeSpinner.setSelection(PositionMode.CONSERVATIVE.ordinal)
        root.addView(modeSpinner, lp())

        val run = Button(this).apply {
            text = "随机起盘并冻结预测"
            setOnClickListener { runPrediction() }
        }
        root.addView(run, lp())

        summaryOutput = TextView(this).apply {
            text = "合参结论将在起盘后显示"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextIsSelectable(true)
            setPadding(0, dp(14), 0, dp(6))
        }
        root.addView(summaryOutput)

        output = TextView(this).apply {
            text = "尚未起盘"
            textSize = 16f
            setTextIsSelectable(true)
            setPadding(0, dp(8), 0, dp(12))
        }
        root.addView(output)

        val lock = Button(this).apply {
            text = "保存本次预测（冻结）"
            setOnClickListener { savePrediction() }
        }
        root.addView(lock, lp())

        root.addView(TextView(this).apply {
            text = "赛果复盘"
            textSize = 20f
            setPadding(0, dp(20), 0, dp(8))
        })

        val scoreRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        homeScoreInput = numberEdit("主")
        awayScoreInput = numberEdit("客")
        scoreRow.addView(homeScoreInput, LinearLayout.LayoutParams(0, dp(52), 1f))
        scoreRow.addView(TextView(this).apply { text = "  :  "; textSize = 24f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(50), dp(52)))
        scoreRow.addView(awayScoreInput, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(scoreRow, lp())

        root.addView(Button(this).apply {
            text = "录入赛果并自动复盘"
            setOnClickListener { saveActualResult() }
        }, lp())

        historyText = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(0, dp(16), 0, dp(8))
        }
        root.addView(historyText)

        root.addView(TextView(this).apply {
            text = "说明：本工具用于传统术数模型的文化研究与盲测记录，不代表具备稳定预测能力，不构成投注建议。四箭古籍口诀存在异文，v0.1 暂不计入胜负加权。"
            textSize = 12f
            setPadding(0, dp(14), 0, 0)
        })

        refreshHistory()
    }

    private fun runPrediction() {
        val mode = PositionMode.values()[modeSpinner.selectedItemPosition]
        val prediction = LeiEngine.predict(
            homeInput.text.toString().trim(),
            awayInput.text.toString().trim(),
            mode
        )
        lastPrediction = prediction
        lastRecordIndex = -1
        summaryOutput.text = renderConsensus(prediction)
        output.text = renderPrediction(prediction)
    }

    private fun renderConsensus(p: Prediction): String = buildString {
        appendLine("三、合参结论")
        appendLine("结构：${p.structure}")
        appendLine("胜平负：${p.resultOrder}")
        appendLine("不败：${p.unbeaten}")
        appendLine("总进球：${p.goals}球核心  ${p.bigSmall}  ${p.oddEven}")
        appendLine("半全场：${p.halfFull}")
        appendLine("比分：${p.scores.joinToString(" / ")}")
        append("信心：${p.confidence}")
    }

    private fun renderPrediction(p: Prediction): String {
        val c = p.current
        val r = p.random
        return buildString {
            appendLine("【${p.home} vs ${p.away}】")
            appendLine("定位：${p.mode.label}")
            appendLine()
            appendLine("一、正时背景盘")
            appendLine("时间：${c.time.format(fmt)}  UTC+8")
            appendLine("四柱：${c.baZi}")
            appendLine("年星：${c.yearStar.name}(${c.yearStar.element})")
            appendLine("日层：${c.dayStar.name}(${c.dayStar.element})  时层：${c.timeStar.name}(${c.timeStar.element})")
            appendLine("直符：${c.directPalace}  传音：${c.transmissionPalace}  中宫箭：${c.middleArrows.first}/${c.middleArrows.second}")
            appendLine("刻盘：第${c.microIndex + 1}刻 · ${c.microPalace}(${c.microElement})")
            appendLine("封盘码：${c.seal}")
            appendLine()
            appendLine("二、完全随机盘")
            appendLine("时间：${r.time.format(fmt)}  UTC+8")
            appendLine("四柱：${r.baZi}")
            appendLine("年星：${r.yearStar.name}(${r.yearStar.element})")
            appendLine("日层：${r.dayStar.name}(${r.dayStar.element})  时层：${r.timeStar.name}(${r.timeStar.element})")
            appendLine("直符：${r.directPalace}  传音：${r.transmissionPalace}  中宫箭：${r.middleArrows.first}/${r.middleArrows.second}")
            appendLine("刻盘：第${r.microIndex + 1}刻 · ${r.microPalace}(${r.microElement})")
            appendLine("封盘码：${r.seal}")
            appendLine()
            appendLine("三、合参结论")
            appendLine("结构：${p.structure}")
            appendLine("胜平负：${p.resultOrder}")
            appendLine("不败：${p.unbeaten}")
            appendLine("总进球：${p.goals}球核心  ${p.bigSmall}  ${p.oddEven}")
            appendLine("半全场：${p.halfFull}")
            appendLine("比分：${p.scores.joinToString(" / ")}")
            appendLine("信心：${p.confidence}")
            appendLine()
            appendLine("四、证据链")
            p.notes.forEach { appendLine("• $it") }
        }
    }

    private fun savePrediction() {
        val p = lastPrediction ?: return toast("请先起盘")
        val arr = loadHistory()
        val obj = JSONObject().apply {
            put("home", p.home)
            put("away", p.away)
            put("mode", p.mode.name)
            put("currentTime", p.current.time.format(fmt))
            put("randomTime", p.random.time.format(fmt))
            put("currentSeal", p.current.seal)
            put("randomSeal", p.random.seal)
            put("order", p.resultOrder)
            put("unbeaten", p.unbeaten)
            put("goals", p.goals)
            put("bigSmall", p.bigSmall)
            put("oddEven", p.oddEven)
            put("halfFull", p.halfFull)
            put("scores", JSONArray(p.scores))
            put("structure", p.structure)
            put("actualHome", JSONObject.NULL)
            put("actualAway", JSONObject.NULL)
        }
        arr.put(obj)
        while (arr.length() > 200) {
            val trimmed = JSONArray()
            for (i in 1 until arr.length()) trimmed.put(arr.getJSONObject(i))
            saveHistory(trimmed)
            lastRecordIndex = trimmed.length() - 1
            refreshHistory()
            toast("已冻结预测")
            return
        }
        saveHistory(arr)
        lastRecordIndex = arr.length() - 1
        refreshHistory()
        toast("已冻结预测")
    }

    private fun saveActualResult() {
        val hs = homeScoreInput.text.toString().toIntOrNull()
        val ascore = awayScoreInput.text.toString().toIntOrNull()
        if (hs == null || ascore == null) return toast("请输入完整赛果")
        val arr = loadHistory()
        var idx = lastRecordIndex
        if (idx !in 0 until arr.length()) {
            for (i in arr.length() - 1 downTo 0) {
                val o = arr.getJSONObject(i)
                if (o.isNull("actualHome")) { idx = i; break }
            }
        }
        if (idx !in 0 until arr.length()) return toast("没有待复盘的冻结预测")
        val obj = arr.getJSONObject(idx)
        obj.put("actualHome", hs)
        obj.put("actualAway", ascore)
        saveHistory(arr)
        lastRecordIndex = idx
        refreshHistory()
        toast("赛果已记录")
    }

    private fun refreshHistory() {
        val arr = loadHistory()
        var reviewed = 0
        var directionHit = 0
        var exactHit = 0
        var sizeHit = 0
        var parityHit = 0
        val recent = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!o.isNull("actualHome") && !o.isNull("actualAway")) {
                reviewed++
                val h = o.getInt("actualHome")
                val a = o.getInt("actualAway")
                val actualDirection = when { h > a -> "主胜"; h < a -> "客胜"; else -> "平" }
                val top = o.getString("order").substringBefore(" > ")
                if (top == actualDirection) directionHit++
                val scoreArr = o.getJSONArray("scores")
                if ((0 until scoreArr.length()).any { scoreArr.getString(it) == "$h:$a" }) exactHit++
                val total = h + a
                val actualSize = if (total >= 3) "偏大" else "偏小"
                if (o.getString("bigSmall") == actualSize) sizeHit++
                val actualParity = if (total % 2 == 0) "双" else "单"
                if (o.getString("oddEven") == actualParity) parityHit++
                if (recent.size < 8) {
                    recent.add(0, "#${i + 1} ${o.getString("home")} ${h}:${a} ${o.getString("away")}｜赛前:${o.getString("order")}")
                }
            }
        }
        historyText.text = buildString {
            appendLine("历史：${arr.length()} 场，已复盘 $reviewed 场")
            if (reviewed > 0) {
                appendLine("第一方向命中：$directionHit/$reviewed (${pct(directionHit, reviewed)})")
                appendLine("候选比分覆盖：$exactHit/$reviewed (${pct(exactHit, reviewed)})")
                appendLine("大小方向命中：$sizeHit/$reviewed (${pct(sizeHit, reviewed)})")
                appendLine("单双命中：$parityHit/$reviewed (${pct(parityHit, reviewed)})")
            }
            if (recent.isNotEmpty()) {
                appendLine()
                appendLine("最近复盘：")
                recent.forEach { appendLine(it) }
            }
        }
    }

    private fun loadHistory(): JSONArray = try {
        JSONArray(prefs.getString("records", "[]"))
    } catch (_: Exception) { JSONArray() }

    private fun saveHistory(arr: JSONArray) {
        prefs.edit().putString("records", arr.toString()).apply()
    }

    private fun pct(a: Int, b: Int): String = if (b == 0) "0%" else "%.1f%%".format(a * 100.0 / b)

    private fun edit(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 16f
        setPadding(dp(12), 0, dp(12), 0)
    }

    private fun numberEdit(hintText: String) = EditText(this).apply {
        hint = hintText
        inputType = InputType.TYPE_CLASS_NUMBER
        gravity = Gravity.CENTER
        textSize = 20f
    }

    private fun lp() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(8)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
