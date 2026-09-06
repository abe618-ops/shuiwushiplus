package com.shuiwushiplus.dualrandom

import java.security.SecureRandom

data class SingleReading(
    val number: String,
    val spreadScore: Int,
    val totalScore: Int,
    val spreadLabel: String,
    val totalLabel: String,
    val detail: String
)

data class PredictionResult(
    val user: SingleReading,
    val random: SingleReading,
    val finalSpread: String,
    val finalTotal: String,
    val confidence: String
)

object PredictionEngine {
    private val secureRandom = SecureRandom()

    fun generateThreeDigit(): String = secureRandom.nextInt(1000).toString().padStart(3, '0')

    fun predict(userNumber: String, randomNumber: String = generateThreeDigit()): PredictionResult {
        require(userNumber.matches(Regex("\\d{3}"))) { "请输入三位数字，例如 077、239、707" }
        require(randomNumber.matches(Regex("\\d{3}")))

        val a = read(userNumber)
        val b = read(randomNumber)
        val spread = if (a.spreadScore + b.spreadScore >= 0) "强者让分胜" else "弱者受让胜"
        val total = if (a.totalScore + b.totalScore >= 0) "大分" else "小分"
        val spreadStrength = kotlin.math.abs(a.spreadScore + b.spreadScore)
        val totalStrength = kotlin.math.abs(a.totalScore + b.totalScore)
        val confidence = when {
            spreadStrength >= 4 && totalStrength >= 4 -> "A"
            spreadStrength >= 2 && totalStrength >= 2 -> "B+"
            spreadStrength >= 1 && totalStrength >= 1 -> "B"
            else -> "C"
        }
        return PredictionResult(a, b, spread, total, confidence)
    }

    /**
     * Frozen offline mapping reconstructed from the current chat workflow.
     * It intentionally stays deterministic and does not adapt after results.
     * Core pieces: Meihua-style upper/lower trigram + moving line, fixed symbolic
     * weights for spread/total, then a second independent number and score fusion.
     */
    private fun read(n: String): SingleReading {
        val d = n.map { it.digitToInt() }
        val upper = trigram(d[0])
        val lower = trigram(d[1])
        val moving = ((d.sum() - 1) % 6) + 1

        var spread = upper.spread + lower.spread
        var total = upper.total + lower.total

        // Frozen moving-line modifiers.
        spread += when (moving) { 1 -> 1; 2 -> -1; 3 -> -1; 4 -> 1; 5 -> 1; else -> -1 }
        total += when (moving) { 1 -> -1; 2 -> -1; 3 -> 1; 4 -> 1; 5 -> -1; else -> 1 }

        // Third-digit modifiers are deliberately fixed and transparent.
        spread += when (d[2]) {
            1, 4, 8, 9 -> 1
            0, 2, 5, 7 -> -1
            else -> 0
        }
        total += when (d[2]) {
            3, 4, 6, 7, 9 -> 1
            0, 1, 2, 5, 8 -> -1
            else -> 0
        }

        // Pair interaction layer matching the stable verbal mapping used so far.
        val pair = upper.name + lower.name
        when (pair) {
            "震震", "乾震", "震乾", "乾乾" -> spread += 2
            "艮坤", "坤艮", "兑坎", "坎兑" -> spread -= 2
            "坎离", "离坎" -> spread -= 1
        }
        when (pair) {
            "震震", "兑离", "离兑", "坤离", "离坤" -> total += 2
            "艮坤", "坤艮", "乾坎", "坎乾" -> total -= 2
        }

        val spreadLabel = if (spread >= 0) "强者" else "弱者"
        val totalLabel = if (total >= 0) "大" else "小"
        val detail = "${upper.name}/${lower.name} · 动${moving}爻 · 原始：$spreadLabel/$totalLabel"
        return SingleReading(n, spread, total, spreadLabel, totalLabel, detail)
    }

    private data class Trigram(val name: String, val spread: Int, val total: Int)

    private fun trigram(v: Int): Trigram = when (v % 8) {
        1 -> Trigram("乾", 2, 0)
        2 -> Trigram("兑", -1, 1)
        3 -> Trigram("离", 0, 2)
        4 -> Trigram("震", 2, 2)
        5 -> Trigram("巽", 0, 1)
        6 -> Trigram("坎", -1, -2)
        7 -> Trigram("艮", -2, -2)
        else -> Trigram("坤", -1, -2)
    }
}
