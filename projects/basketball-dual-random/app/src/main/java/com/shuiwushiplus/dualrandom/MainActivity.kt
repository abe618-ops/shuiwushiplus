package com.shuiwushiplus.dualrandom

import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = "双维起盘 · 篮球"
            textSize = 28f
        }
        val sub = TextView(this).apply {
            text = "本地冻结内核｜用户三位数 + 安全随机三位数｜无需联网/ChatGPT"
            textSize = 14f
        }
        val input = EditText(this).apply {
            hint = "输入三位数字，例如 077"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3))
        }
        val randomText = TextView(this).apply {
            text = "随机维：待生成"
            textSize = 18f
        }
        val btn = MaterialButton(this).apply { text = "自动起盘并预测" }

        val resultCard = MaterialCardView(this)
        val result = TextView(this).apply {
            setPadding(pad, pad, pad, pad)
            textSize = 20f
            gravity = Gravity.START
            text = "输入三位数后点击起盘"
        }
        resultCard.addView(result)

        val detail = TextView(this).apply { textSize = 14f }

        btn.setOnClickListener {
            val user = input.text.toString().trim()
            if (!user.matches(Regex("\\d{3}"))) {
                input.error = "请输入完整三位数字，可包含前导0"
                return@setOnClickListener
            }
            try {
                val r = PredictionEngine.predict(user)
                randomText.text = "随机维：${r.random.number}（本机安全随机）"
                result.text = "让分：${r.finalSpread}\n大小：${r.finalTotal}\n信号：${r.confidence}"
                detail.text = "输入维 ${r.user.number}：${r.user.detail}\n\n随机维 ${r.random.number}：${r.random.detail}\n\n双维统算：让分分值 ${r.user.spreadScore + r.random.spreadScore}；大小分值 ${r.user.totalScore + r.random.totalScore}\n\n注：这是固定规则的术数实验工具，不会根据赛果自动改模型。"
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "起盘失败", Toast.LENGTH_SHORT).show()
            }
        }

        root.addView(title)
        root.addView(sub)
        root.addView(space(12))
        root.addView(input)
        root.addView(btn)
        root.addView(space(12))
        root.addView(randomText)
        root.addView(space(12))
        root.addView(resultCard)
        root.addView(space(12))
        root.addView(detail)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun space(dp: Int): Space = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, (dp * resources.displayMetrics.density).toInt())
    }
}
