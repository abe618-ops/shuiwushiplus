package com.shuiwushiplus.leiformula

import com.nlf.calendar.Solar
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class Element { 木, 火, 土, 金, 水, 混合 }

data class LeiStar(val name: String, val element: Element, val tag: String)

data class Plate(
    val source: String,
    val time: LocalDateTime,
    val baZi: String,
    val yearGanZhi: String,
    val monthGanZhi: String,
    val dayGanZhi: String,
    val timeGanZhi: String,
    val yearStar: LeiStar,
    val dayStar: LeiStar,
    val timeStar: LeiStar,
    val directPalace: String,
    val transmissionPalace: String,
    val middleArrows: Pair<String, String>,
    val microIndex: Int,
    val microPalace: String,
    val microElement: Element,
    val seal: String
)

data class Prediction(
    val home: String,
    val away: String,
    val mode: PositionMode,
    val current: Plate,
    val random: Plate,
    val resultOrder: String,
    val unbeaten: String,
    val goals: Int,
    val bigSmall: String,
    val oddEven: String,
    val halfFull: String,
    val scores: List<String>,
    val confidence: String,
    val structure: String,
    val notes: List<String>
)

enum class PositionMode(val label: String) {
    M1("M1：日星=主 / 时星=客（旧模型对照）"),
    REVERSE("M2：时星=主 / 日星=客（反向实验）"),
    CONSERVATIVE("双模型保守合参（默认）")
}

object LeiEngine {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val rng = SecureRandom()

    // 雷霆曜气十二星固定顺序。日支映射采用本项目校勘后的 V2 表。
    private val stars = listOf(
        LeiStar("血刃", Element.金, "攻杀/锋利"),
        LeiStar("太阳", Element.火, "和控/稳定"),
        LeiStar("月孛", Element.水, "暗变/扰动"),
        LeiStar("金水", Element.水, "和控/平衡"),
        LeiStar("台将", Element.土, "贵气/执行"),
        LeiStar("天罡", Element.土, "刚健/变化"),
        LeiStar("土溽", Element.土, "滞缓/低效"),
        LeiStar("奇罗", Element.火, "吉曜/冲击"),
        LeiStar("燥火", Element.火, "躁烈/破局"),
        LeiStar("丙乙", Element.混合, "细腻/偏弱"),
        LeiStar("水潦", Element.水, "沉滞/两极"),
        LeiStar("紫气", Element.木, "和缓/生发")
    )
    private val starByName = stars.associateBy { it.name }

    private val dayStarByZhi = mapOf(
        "子" to "太阳", "丑" to "血刃", "寅" to "紫气", "卯" to "水潦",
        "辰" to "丙乙", "巳" to "燥火", "午" to "奇罗", "未" to "土溽",
        "申" to "天罡", "酉" to "台将", "戌" to "金水", "亥" to "月孛"
    )
    private val yearStarByGan = mapOf(
        "甲" to "血刃", "庚" to "血刃", "乙" to "太阳", "辛" to "太阳",
        "丙" to "金水", "壬" to "金水", "丁" to "月孛", "癸" to "月孛",
        "戊" to "紫气", "己" to "台将"
    )
    private val timeStartByDayGan = mapOf(
        "甲" to "燥火", "己" to "燥火", "乙" to "太阳", "庚" to "太阳",
        "丙" to "天罡", "辛" to "天罡", "丁" to "月孛", "壬" to "月孛",
        "戊" to "紫气", "癸" to "紫气"
    )

    private val fly9 = listOf("乾", "兑", "艮", "离", "坎", "坤", "震", "巽", "中")
    private val transmission9 = listOf("艮", "兑", "乾", "中", "巽", "震", "坤", "坎", "离")
    private val micro8 = listOf("乾", "兑", "艮", "离", "坎", "坤", "震", "巽")
    private val palaceElement = mapOf(
        "乾" to Element.金, "兑" to Element.金, "艮" to Element.土, "坤" to Element.土,
        "离" to Element.火, "坎" to Element.水, "震" to Element.木, "巽" to Element.木,
        "中" to Element.土
    )
    private val zhiOrder = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val ganOrder = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val jiaZi = List(60) { i -> ganOrder[i % 10] + zhiOrder[i % 12] }

    fun now(): LocalDateTime = LocalDateTime.now(zone)

    fun randomTime1900to2099(): LocalDateTime {
        val start = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2099, 12, 31)
        val days = (end.toEpochDay() - start.toEpochDay() + 1).toInt()
        val d = start.plusDays(rng.nextInt(days).toLong())
        val minute = rng.nextInt(24 * 60)
        return d.atTime(minute / 60, minute % 60)
    }

    fun plate(source: String, time: LocalDateTime): Plate {
        val solar = Solar.fromYmdHms(time.year, time.monthValue, time.dayOfMonth, time.hour, time.minute, time.second)
        val eight = solar.lunar.eightChar
        val yearGz = eight.year
        val monthGz = eight.month
        val dayGz = eight.day
        val timeGz = eight.time
        val yearGan = yearGz.substring(0, 1)
        val dayGan = dayGz.substring(0, 1)
        val dayZhi = dayGz.substring(1, 2)
        val timeZhi = timeGz.substring(1, 2)

        val yearStar = star(yearStarByGan[yearGan] ?: "太阳")
        val dayStar = star(dayStarByZhi[dayZhi] ?: "太阳")
        val timeStar = calculateTimeStar(dayGan, timeZhi)
        val direct = directPalace(dayGz)
        val transmission = transmissionPalace(dayZhi)
        val arrows = middleArrows(yearGan)
        val micro = micro(time)
        val bz = "$yearGz $monthGz $dayGz $timeGz"
        val seal = sha256("$source|${time.format(fmt)}|$bz|${dayStar.name}|${timeStar.name}").take(12)

        return Plate(
            source, time, bz, yearGz, monthGz, dayGz, timeGz,
            yearStar, dayStar, timeStar, direct, transmission, arrows,
            micro.first, micro.second, palaceElement[micro.second] ?: Element.土, seal
        )
    }

    fun predict(home: String, away: String, mode: PositionMode, currentTime: LocalDateTime = now(), randomTime: LocalDateTime = randomTime1900to2099()): Prediction {
        val current = plate("正时", currentTime)
        val random = plate("随机", randomTime)
        var h = 0.0
        var d = 0.0
        var a = 0.0
        val notes = mutableListOf<String>()

        // 正时仅作背景（V2.3：避免密集比赛同一时辰反复支配结果）
        val currentRel = relation(current.dayStar.element, current.timeStar.element)
        if (current.dayStar.name == current.timeStar.name) {
            d += 2.4
            notes += "正时同星：${current.dayStar.name}-${current.timeStar.name}，强平背景"
        } else if (current.dayStar.element == current.timeStar.element) {
            d += 1.5
            notes += "正时同五行：平衡权重上升"
        } else {
            if (currentRel == Relation.A_CONTROLS_B || currentRel == Relation.B_CONTROLS_A) notes += "正时存在克制，仅记为环境扰动，不直接判主客"
        }

        // 随机盘是主要区分盘。M1 与反向 M2 可独立盲测。
        val randomRel = relation(random.dayStar.element, random.timeStar.element)
        when (mode) {
            PositionMode.M1 -> applyRelation(randomRel, 2.5) { dh, dd, da -> h += dh; d += dd; a += da }
            PositionMode.REVERSE -> applyRelation(reverse(randomRel), 2.5) { dh, dd, da -> h += dh; d += dd; a += da }
            PositionMode.CONSERVATIVE -> {
                val m1 = relationVector(randomRel, 2.2)
                val m2 = relationVector(reverse(randomRel), 2.2)
                h += (m1.first + m2.first) / 2.0
                d += (m1.second + m2.second) / 2.0 + if (m1.first != m2.first || m1.third != m2.third) 0.8 else 0.0
                a += (m1.third + m2.third) / 2.0
                notes += "主客定位尚未定型：默认双模型保守合参，分歧时自动抬高平局"
            }
        }

        if (random.dayStar.name == random.timeStar.name) d += 1.8
        else if (random.dayStar.element == random.timeStar.element) d += 1.0

        // 刻盘只做微调，不允许单独翻转强方向。
        h += microAssist(random.microElement, if (mode == PositionMode.REVERSE) random.timeStar.element else random.dayStar.element)
        a += microAssist(random.microElement, if (mode == PositionMode.REVERSE) random.dayStar.element else random.timeStar.element)

        val arrowHits = listOf(current.dayGanZhi.takeLast(1), current.timeGanZhi.takeLast(1)).count { it == current.middleArrows.first || it == current.middleArrows.second } +
            listOf(random.dayGanZhi.takeLast(1), random.timeGanZhi.takeLast(1)).count { it == random.middleArrows.first || it == random.middleArrows.second }
        if (arrowHits > 0) notes += "中宫箭触发 $arrowHits 次：只提高变局/事件权重，不再等同于必破平"

        notes += "直符：正时${current.directPalace}、随机${random.directPalace}；传音：正时${current.transmissionPalace}、随机${random.transmissionPalace}"
        notes += "四箭：因现存口诀存在异文，v0.1 仅保留模块位，不参与胜负加权，避免伪造确定算法"

        val order = order(h, d, a)
        val top = listOf("主" to h, "平" to d, "客" to a).sortedByDescending { it.second }
        val confidence = when {
            top[0].second - top[1].second >= 2.0 -> "中高"
            top[0].second - top[1].second >= 0.8 -> "中"
            else -> "低/分歧"
        }
        val result = top.first().first

        var goalIndex = 2.0
        if (current.dayStar.name == current.timeStar.name) goalIndex -= 0.8
        else if (current.dayStar.element == current.timeStar.element) goalIndex -= 0.4
        if (random.dayStar.name == random.timeStar.name) goalIndex -= 0.6
        else if (random.dayStar.element == random.timeStar.element) goalIndex -= 0.25
        if (randomRel == Relation.A_CONTROLS_B || randomRel == Relation.B_CONTROLS_A) goalIndex += 0.65
        if (arrowHits > 0) goalIndex += 0.18 * arrowHits
        if (random.microElement == Element.火) goalIndex += 0.2
        if (random.microElement == Element.土 || random.microElement == Element.水) goalIndex -= 0.1
        val goals = goalIndex.roundToInt().coerceIn(0, 4)

        val scores = scoreCandidates(result, goals)
        val unbeaten = when {
            result == "平" && h >= a -> "主不败（但平局为核心）"
            result == "平" -> "客不败（但平局为核心）"
            result == "主" -> "主不败"
            else -> "客不败"
        }
        val structure = when {
            current.dayStar.name == current.timeStar.name && random.dayStar.element == random.timeStar.element -> "双维平衡/低破局"
            current.dayStar.name == current.timeStar.name -> "正时强平，随机盘负责分流"
            randomRel == Relation.A_CONTROLS_B || randomRel == Relation.B_CONTROLS_A -> "随机盘有克制，偏分胜负"
            else -> "制衡/流向局"
        }

        return Prediction(
            home.ifBlank { "主队" }, away.ifBlank { "客队" }, mode, current, random,
            order, unbeaten, goals, if (goals >= 3) "偏大" else "偏小", if (goals % 2 == 0) "双" else "单",
            when (result) { "主" -> "平/胜"; "客" -> "平/负"; else -> "平/平" },
            scores, confidence, structure, notes
        )
    }

    private fun star(name: String) = starByName.getValue(name)

    private fun calculateTimeStar(dayGan: String, timeZhi: String): LeiStar {
        val start = star(timeStartByDayGan[dayGan] ?: "太阳")
        val step = when (timeZhi) {
            "丑", "巳", "酉" -> 2 // 兑
            "寅", "午", "戌" -> 4 // 离
            "申", "子", "辰" -> 5 // 坎
            "亥", "卯", "未" -> 7 // 震
            else -> 0
        }
        val index = (stars.indexOfFirst { it.name == start.name } + step) % stars.size
        return stars[index]
    }

    private fun directPalace(dayGz: String): String {
        val idx = jiaZi.indexOf(dayGz).let { if (it < 0) 0 else it }
        return fly9[idx % fly9.size]
    }

    private fun transmissionPalace(dayZhi: String): String {
        val start = zhiOrder.indexOf("寅")
        val end = zhiOrder.indexOf(dayZhi)
        val offset = (end - start + 12) % 12
        return transmission9[offset % transmission9.size]
    }

    private fun middleArrows(yearGan: String): Pair<String, String> = when (yearGan) {
        "甲", "己" -> "巳" to "戌"
        "乙", "庚" -> "辰" to "酉"
        "丙", "辛" -> "卯" to "申"
        "丁", "壬" -> "未" to "寅"
        else -> "午" to "亥" // 戊癸
    }

    private fun micro(t: LocalDateTime): Pair<Int, String> {
        val branchIndex = ((t.hour + 1) / 2) % 12
        val startMinute = if (branchIndex == 0) 23 * 60 else (branchIndex * 2 - 1) * 60
        var nowMinute = t.hour * 60 + t.minute
        if (branchIndex == 0 && t.hour == 0) nowMinute += 24 * 60
        val offset = (nowMinute - startMinute).coerceIn(0, 119)
        val index = (offset / 15).coerceIn(0, 7)
        return index to micro8[index]
    }

    private enum class Relation { SAME, A_CONTROLS_B, B_CONTROLS_A, A_GENERATES_B, B_GENERATES_A, NEUTRAL }

    private fun relation(a: Element, b: Element): Relation {
        if (a == Element.混合 || b == Element.混合) return Relation.NEUTRAL
        if (a == b) return Relation.SAME
        if (controls(a, b)) return Relation.A_CONTROLS_B
        if (controls(b, a)) return Relation.B_CONTROLS_A
        if (generates(a, b)) return Relation.A_GENERATES_B
        if (generates(b, a)) return Relation.B_GENERATES_A
        return Relation.NEUTRAL
    }

    private fun controls(a: Element, b: Element) = when (a) {
        Element.木 -> b == Element.土
        Element.土 -> b == Element.水
        Element.水 -> b == Element.火
        Element.火 -> b == Element.金
        Element.金 -> b == Element.木
        else -> false
    }

    private fun generates(a: Element, b: Element) = when (a) {
        Element.木 -> b == Element.火
        Element.火 -> b == Element.土
        Element.土 -> b == Element.金
        Element.金 -> b == Element.水
        Element.水 -> b == Element.木
        else -> false
    }

    private fun reverse(r: Relation) = when (r) {
        Relation.A_CONTROLS_B -> Relation.B_CONTROLS_A
        Relation.B_CONTROLS_A -> Relation.A_CONTROLS_B
        Relation.A_GENERATES_B -> Relation.B_GENERATES_A
        Relation.B_GENERATES_A -> Relation.A_GENERATES_B
        else -> r
    }

    private fun relationVector(r: Relation, w: Double): Triple<Double, Double, Double> = when (r) {
        Relation.SAME -> Triple(0.0, w, 0.0)
        Relation.A_CONTROLS_B -> Triple(w, 0.1, 0.0)
        Relation.B_CONTROLS_A -> Triple(0.0, 0.1, w)
        // “生”只表示流向/主动性，不再像旧版那样直接视为受生方胜。
        Relation.A_GENERATES_B -> Triple(w * 0.32, w * 0.30, 0.0)
        Relation.B_GENERATES_A -> Triple(0.0, w * 0.30, w * 0.32)
        Relation.NEUTRAL -> Triple(0.1, w * 0.35, 0.1)
    }

    private fun applyRelation(r: Relation, w: Double, f: (Double, Double, Double) -> Unit) {
        val v = relationVector(r, w)
        f(v.first, v.second, v.third)
    }

    private fun microAssist(micro: Element, team: Element): Double {
        if (micro == Element.混合 || team == Element.混合) return 0.0
        return when {
            micro == team -> 0.22
            generates(micro, team) -> 0.18
            controls(micro, team) -> -0.12
            else -> 0.0
        }
    }

    private fun order(h: Double, d: Double, a: Double): String = listOf("主胜" to h, "平" to d, "客胜" to a)
        .sortedByDescending { it.second }.joinToString(" > ") { it.first }

    private fun scoreCandidates(result: String, goals: Int): List<String> = when (result) {
        "主" -> when (goals) {
            0, 1 -> listOf("1:0", "0:0", "2:0")
            2 -> listOf("1:0", "2:0", "1:1")
            3 -> listOf("2:1", "2:0", "1:0")
            else -> listOf("3:1", "2:1", "3:0")
        }
        "客" -> when (goals) {
            0, 1 -> listOf("0:1", "0:0", "0:2")
            2 -> listOf("0:1", "0:2", "1:1")
            3 -> listOf("1:2", "0:2", "0:1")
            else -> listOf("1:3", "1:2", "0:3")
        }
        else -> when (goals) {
            0, 1 -> listOf("0:0", "1:1", "1:0")
            2 -> listOf("1:1", "0:0", "1:0")
            else -> listOf("1:1", "2:2", "0:0")
        }
    }

    private fun sha256(s: String): String = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
