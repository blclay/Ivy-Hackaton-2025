package com.example.moodrise

import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.moodrise.data.PrefsStorage
import com.example.moodrise.model.Mood
import java.time.LocalDate

class CalendarActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsStorage
    private lateinit var streakText: TextView
    private lateinit var daySummary: TextView
    private lateinit var calendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        prefs = PrefsStorage(this)

        streakText = findViewById(R.id.streakText)
        daySummary = findViewById(R.id.daySummary)
        calendarView = findViewById(R.id.calendar)

        val streak = prefs.computeStreak()
        streakText.text = "Good-mood streak: $streak days"

        val today = LocalDate.now()
        showSummaryForDate(today)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = LocalDate.of(year, month + 1, dayOfMonth)
            showSummaryForDate(date)
        }
    }

    private fun showSummaryForDate(date: LocalDate) {
        val (start, end) = prefs.getDayLog(date)
        val startText = moodToFriendly(start)
        val endText = moodToFriendly(end)
        daySummary.text = "Date: $date\nStart: $startText   End: $endText"

        val color = trendColor(start, end)
        daySummary.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun moodToFriendly(mood: Mood?): String {
        return when (mood) {
            Mood.GREAT -> "Great"
            Mood.GOOD  -> "Good"
            Mood.OKAY  -> "Okay"
            Mood.MEH   -> "Meh"
            Mood.LOW   -> "Bad"
            null       -> "—"
        }
    }

    private fun trendColor(start: Mood?, end: Mood?): Int {
        if (start == null || end == null) return R.color.textSecondary
        return when {
            end.ordinal < start.ordinal -> R.color.brandGreen
            end.ordinal > start.ordinal -> android.R.color.holo_red_light
            else -> R.color.textSecondary
        }
    }
}
