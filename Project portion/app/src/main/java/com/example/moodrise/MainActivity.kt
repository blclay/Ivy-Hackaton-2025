package com.example.moodrise

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.moodrise.data.PrefsStorage
import com.example.moodrise.model.Category
import com.example.moodrise.model.Mood
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsStorage

    private lateinit var moodLabel: TextView
    private lateinit var moodSlider: SeekBar
    private lateinit var e0: TextView
    private lateinit var e1: TextView
    private lateinit var e2: TextView
    private lateinit var e3: TextView
    private lateinit var e4: TextView
    private lateinit var btnEducate: Button
    private lateinit var btnLaugh: Button
    private lateinit var btnMotivate: Button
    private lateinit var btnCalendar: Button

    private var wellnessTimer: CountDownTimer? = null
    private var recurringTimer: CountDownTimer? = null

    // Session state
    private var hasChosenMoodThisSession: Boolean = false
    private var startLoggedFromHomeThisDay: Boolean = false
    private var userIsTouchingSlider: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsStorage(this)

        // For your demo: reset today's data on app launch
        prefs.resetForNewLaunch()
        startLoggedFromHomeThisDay = false

        // Bind views
        moodLabel   = findViewById(R.id.moodLabel)
        moodSlider  = findViewById(R.id.moodSlider)
        e0          = findViewById(R.id.e0)
        e1          = findViewById(R.id.e1)
        e2          = findViewById(R.id.e2)
        e3          = findViewById(R.id.e3)
        e4          = findViewById(R.id.e4)
        btnEducate  = findViewById(R.id.btnEducate)
        btnLaugh    = findViewById(R.id.btnLaugh)
        btnMotivate = findViewById(R.id.btnMotivate)
        btnCalendar = findViewById(R.id.btnCalendar)

        setFeedButtonsEnabled(false)

        // Display starts at "Okay" but we don't log anything until the user finishes interacting
        val initialIndex = 2 // Okay
        moodSlider.progress = initialIndex
        applyMoodFromSlider(initialIndex, persist = false)

        moodSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Update current mood as they drag; DO NOT log START/END here.
                    applyMoodFromSlider(progress, persist = true)

                    if (!hasChosenMoodThisSession) {
                        hasChosenMoodThisSession = true
                        setFeedButtonsEnabled(true)
                        Toast.makeText(this@MainActivity, "Mood set. You can open a feed now.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    applyMoodFromSlider(progress, persist = false)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                userIsTouchingSlider = true
                // Enable buttons on first touch so they can proceed
                if (!hasChosenMoodThisSession) {
                    hasChosenMoodThisSession = true
                    setFeedButtonsEnabled(true)
                }
                // IMPORTANT: Do NOT log START here (value might still be the old one).
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                userIsTouchingSlider = false
                // Now the slider has its final value; if START not logged yet today,
                // log it using the slider's final position.
                if (!startLoggedFromHomeThisDay) {
                    val startMood = moodFromSlider(moodSlider.progress)
                    prefs.logStartMoodIfEmpty(LocalDate.now(), startMood)
                    startLoggedFromHomeThisDay = true
                }
            }
        })

        startWellnessTimers()

        btnEducate.setOnClickListener { tryOpenFeed(Category.EDUCATE) }
        btnLaugh.setOnClickListener   { tryOpenFeed(Category.LAUGH) }
        btnMotivate.setOnClickListener{ tryOpenFeed(Category.MOTIVATE) }

        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wellnessTimer?.cancel()
        recurringTimer?.cancel()
    }

    // ---------------- UI helpers ----------------

    private fun setFeedButtonsEnabled(enabled: Boolean) {
        btnEducate.isEnabled = enabled
        btnLaugh.isEnabled = enabled
        btnMotivate.isEnabled = enabled
    }

    private fun applyMoodFromSlider(indexRaw: Int, persist: Boolean) {
        val index = indexRaw.coerceIn(0, 4)
        val label: String
        val mood: Mood
        when (index) {
            0 -> { label = "Bad";  mood = Mood.LOW }
            1 -> { label = "Meh";  mood = Mood.MEH }
            2 -> { label = "Okay"; mood = Mood.OKAY }
            3 -> { label = "Good"; mood = Mood.GOOD }
            else -> { label = "Great"; mood = Mood.GREAT }
        }
        moodLabel.text = "Mood: $label"
        highlightEmoji(index)
        if (persist) {
            // Save ONLY current mood; START/END are handled elsewhere
            prefs.saveCurrentMood(mood)
        }
    }

    private fun highlightEmoji(activeIdx: Int) {
        val primary   = ContextCompat.getColor(this, R.color.textPrimary)
        val secondary = ContextCompat.getColor(this, R.color.textSecondary)
        listOf(e0, e1, e2, e3, e4).forEachIndexed { i, tv ->
            val on = (i == activeIdx)
            tv.setTextColor(if (on) primary else secondary)
            tv.scaleX = if (on) 1.15f else 1f
            tv.scaleY = if (on) 1.15f else 1f
        }
    }

    // ---------------- Open feed: safety to ensure START exists ----------------

    private fun tryOpenFeed(cat: Category) {
        if (!hasChosenMoodThisSession) {
            Toast.makeText(this, "Please set your mood with the slider first.", Toast.LENGTH_SHORT).show()
            return
        }
        // If user never released the slider (edge case), ensure START is set now with final value.
        if (!startLoggedFromHomeThisDay) {
            val startMood = moodFromSlider(moodSlider.progress)
            prefs.logStartMoodIfEmpty(LocalDate.now(), startMood)
            startLoggedFromHomeThisDay = true
        }

        val i = Intent(this, FeedActivity::class.java)
        i.putExtra("EXTRA_CATEGORY", cat.name)
        startActivity(i)
    }

    private fun moodFromSlider(indexRaw: Int): Mood {
        return when (indexRaw.coerceIn(0, 4)) {
            0 -> Mood.LOW
            1 -> Mood.MEH
            2 -> Mood.OKAY
            3 -> Mood.GOOD
            else -> Mood.GREAT
        }
    }

    // ---------------- Wellness timers / quick check-ins ----------------

    private fun startWellnessTimers() {
        wellnessTimer?.cancel()
        recurringTimer?.cancel()
        wellnessTimer = object : CountDownTimer(5 * 60_000L, 5 * 60_000L) {
            override fun onTick(ms: Long) {}
            override fun onFinish() {
                showQuickCheckIn()
                startRecurringCheckIns(15)
            }
        }.start()
    }

    private fun startRecurringCheckIns(minutes: Int) {
        recurringTimer?.cancel()
        recurringTimer = object : CountDownTimer(minutes * 60_000L, minutes * 60_000L) {
            override fun onTick(ms: Long) {}
            override fun onFinish() {
                showQuickCheckIn()
                startRecurringCheckIns(minutes)
            }
        }.start()
    }

    private fun showQuickCheckIn() {
        val moodsArray = arrayOf("Great", "Good", "Okay", "Meh", "Bad")
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Quick check-in")
            .setSingleChoiceItems(moodsArray, 2, null)
            .setPositiveButton("Save") { dlg, _ ->
                val pos = (dlg as androidx.appcompat.app.AlertDialog).listView.checkedItemPosition
                val chosen = when (pos) {
                    0 -> Mood.GREAT
                    1 -> Mood.GOOD
                    2 -> Mood.OKAY
                    3 -> Mood.MEH
                    else -> Mood.LOW
                }
                // Always update current mood
                prefs.saveCurrentMood(chosen)
                // Only update END if START exists (keeps START = beginning slider mood)
                val today = LocalDate.now()
                if (prefs.hasStartFor(today)) {
                    prefs.logEndMood(today, chosen)
                }

                // Reflect in slider/emoji
                val idx = when (chosen) {
                    Mood.LOW -> 0
                    Mood.MEH -> 1
                    Mood.OKAY -> 2
                    Mood.GOOD -> 3
                    Mood.GREAT -> 4
                }
                moodSlider.progress = idx
                applyMoodFromSlider(idx, persist = false)

                hasChosenMoodThisSession = true
                setFeedButtonsEnabled(true)
            }
            .setNegativeButton("Skip", null)
            .create()
        dialog.show()
    }
}
