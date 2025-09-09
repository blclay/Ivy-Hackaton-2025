package com.example.moodrise.data

import android.content.Context
import com.example.moodrise.model.Mood
import java.time.LocalDate

class PrefsStorage(context: Context) {

    private val sp = context.getSharedPreferences("moodrise_prefs", Context.MODE_PRIVATE)

    // ---------------- Current mood (does NOT touch START/END) ----------------

    /** Save just the current mood shown in the UI. Does NOT modify START/END. */
    fun saveCurrentMood(mood: Mood?) {
        if (mood == null) {
            sp.edit().remove(KEY_CURRENT_MOOD).apply()
        } else {
            sp.edit().putString(KEY_CURRENT_MOOD, mood.name).apply()
        }
    }

    fun getCurrentMood(): Mood? {
        val v = sp.getString(KEY_CURRENT_MOOD, null) ?: return null
        return runCatching { Mood.valueOf(v) }.getOrNull()
    }

    // ---------------- Start / End logs ----------------

    /** Log START only if it's not already set for that day. */
    fun logStartMoodIfEmpty(date: LocalDate, mood: Mood) {
        val dk = dateKey(date)
        val key = "START_$dk"
        if (!sp.contains(key)) {
            sp.edit().putString(key, mood.name).apply()
        }
    }

    /** Explicitly set END for a given day. */
    fun logEndMood(date: LocalDate, mood: Mood) {
        val dk = dateKey(date)
        sp.edit().putString("END_$dk", mood.name).apply()
    }

    /** True if START has been logged for the given date. */
    fun hasStartFor(date: LocalDate): Boolean {
        val dk = dateKey(date)
        return sp.contains("START_$dk")
    }

    /** Returns (START, END) for a date — each may be null. */
    fun getDayLog(date: LocalDate): Pair<Mood?, Mood?> {
        val dk = dateKey(date)
        val s = sp.getString("START_$dk", null)
        val e = sp.getString("END_$dk", null)
        val start = s?.let { runCatching { Mood.valueOf(it) }.getOrNull() }
        val end   = e?.let { runCatching { Mood.valueOf(it) }.getOrNull() }
        return start to end
    }

    // ---------------- Streak: consecutive days where END is OKAY/GOOD/GREAT ----------------

    fun computeStreak(): Int {
        var days = 0
        var cursor = LocalDate.now()
        while (true) {
            val (_, end) = getDayLog(cursor)
            val good = end == Mood.OKAY || end == Mood.GOOD || end == Mood.GREAT
            if (good) {
                days += 1
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        return days
    }

    // ---------------- Daily cap tracking ----------------

    fun getDailyCapMinutes(): Int = sp.getInt(KEY_DAILY_CAP_MIN, 60)

    fun setDailyCapMinutes(min: Int) {
        sp.edit().putInt(KEY_DAILY_CAP_MIN, min).apply()
    }

    fun getMinutesUsedToday(): Int {
        val k = "MINUTES_${dateKey(LocalDate.now())}"
        return sp.getInt(k, 0)
    }

    fun addMinutesUsedToday(delta: Int) {
        val k = "MINUTES_${dateKey(LocalDate.now())}"
        val cur = sp.getInt(k, 0)
        sp.edit().putInt(k, cur + delta).apply()
    }

    // ---------------- Reset (today only) for demo ----------------

    fun resetForNewLaunch() {
        val todayKey = dateKey(LocalDate.now())
        sp.edit()
            .remove("START_$todayKey")
            .remove("END_$todayKey")
            .remove("MINUTES_$todayKey")
            .remove(KEY_CURRENT_MOOD)
            .apply()
    }

    // ---------------- Helpers ----------------

    private fun dateKey(date: LocalDate): String = date.toString() // yyyy-MM-dd

    companion object {
        private const val KEY_CURRENT_MOOD = "CURRENT_MOOD"
        private const val KEY_DAILY_CAP_MIN = "DAILY_CAP_MIN"
    }
}
