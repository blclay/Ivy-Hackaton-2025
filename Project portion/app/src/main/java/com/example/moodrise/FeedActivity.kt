package com.example.moodrise

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.moodrise.data.PrefsStorage
import com.example.moodrise.model.Category
import com.example.moodrise.model.ContentItem
import com.example.moodrise.model.Mood
import java.time.LocalDate
import kotlin.math.max

class FeedActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsStorage
    private lateinit var pager: ViewPager2
    private lateinit var title: TextView
    private lateinit var category: Category

    private var sessionTimer: CountDownTimer? = null
    private var endLoggedThisSession: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        prefs = PrefsStorage(this)

        title = findViewById(R.id.title)
        pager = findViewById(R.id.pager)
        pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        pager.offscreenPageLimit = 1

        category = Category.valueOf(intent.getStringExtra("EXTRA_CATEGORY") ?: Category.EDUCATE.name)
        title.text = when (category) {
            Category.EDUCATE -> "Educate Feed"
            Category.LAUGH   -> "Laugh Feed"
            Category.MOTIVATE-> "Motivate Feed"
        }

        if (prefs.getMinutesUsedToday() >= prefs.getDailyCapMinutes()) {
            startActivity(Intent(this, LockoutActivity::class.java))
            finish()
            return
        }

        val moodNow = prefs.getCurrentMood() ?: Mood.OKAY
        val items = LocalContentRepository.itemsFor(category, moodNow)
        val adapter = CardAdapter(items)
        pager.adapter = adapter

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var last = -1
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (last != -1) adapter.setActive(last, active = false)
                adapter.setActive(position, active = true)
                last = position
            }
        })
        pager.post { adapter.setActive(0, active = true) }

        startSessionTimer()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!endLoggedThisSession) {
                    showEndOfSessionDialog {
                        endLoggedThisSession = true
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionTimer?.cancel()
        if (!endLoggedThisSession) {
            val nowMood = prefs.getCurrentMood() ?: Mood.OKAY
            prefs.logEndMood(LocalDate.now(), nowMood)
            endLoggedThisSession = true
        }
    }

    private fun startSessionTimer() {
        val remaining = max(0, prefs.getDailyCapMinutes() - prefs.getMinutesUsedToday())
        sessionTimer?.cancel()
        sessionTimer = object : CountDownTimer(remaining * 60_000L, 60_000L) {
            override fun onTick(msLeft: Long) {
                prefs.addMinutesUsedToday(1)
            }
            override fun onFinish() {
                if (!endLoggedThisSession) {
                    val nowMood = prefs.getCurrentMood() ?: Mood.OKAY
                    prefs.logEndMood(LocalDate.now(), nowMood)
                    endLoggedThisSession = true
                }
                startActivity(Intent(this@FeedActivity, LockoutActivity::class.java))
                finish()
            }
        }.start()
    }

    private fun showEndOfSessionDialog(onDone: () -> Unit) {
        val moods = arrayOf("Great", "Good", "Okay", "Meh", "Bad")
        AlertDialog.Builder(this)
            .setTitle("Before you go — how do you feel now?")
            .setSingleChoiceItems(moods, 2, null)
            .setPositiveButton("Save") { dialog, _ ->
                val pos = (dialog as AlertDialog).listView.checkedItemPosition
                val mood = when (pos) {
                    0 -> Mood.GREAT
                    1 -> Mood.GOOD
                    2 -> Mood.OKAY
                    3 -> Mood.MEH
                    else -> Mood.LOW
                }
                prefs.logEndMood(LocalDate.now(), mood)
                endLoggedThisSession = true
                onDone()
            }
            .setNegativeButton("Skip") { _, _ ->
                val nowMood = prefs.getCurrentMood() ?: Mood.OKAY
                prefs.logEndMood(LocalDate.now(), nowMood)
                endLoggedThisSession = true
                onDone()
            }
            .setCancelable(false)
            .show()
    }

    private inner class CardAdapter(private val items: List<ContentItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_TEXT = 0
        private val TYPE_VIDEO = 1

        private val players = hashMapOf<Int, ExoPlayer>()
        private var activePos = -1

        override fun getItemViewType(position: Int): Int {
            return if (items[position].type == "video") TYPE_VIDEO else TYPE_TEXT
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_card_video, parent, false)
            return if (viewType == TYPE_VIDEO) VideoVH(v) else TextVH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            if (holder is TextVH) holder.bind(item, position == activePos)
            if (holder is VideoVH) holder.bind(item, position == activePos)
        }

        fun setActive(position: Int, active: Boolean) {
            if (active) {
                if (activePos != -1) players[activePos]?.playWhenReady = false
                activePos = position
                notifyItemChanged(position)
            } else {
                players[position]?.playWhenReady = false
                if (activePos == position) activePos = -1
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            if (holder is VideoVH) {
                val pos = holder.bindingAdapterPosition
                players[pos]?.release()
                players.remove(pos)
            }
        }

        inner class TextVH(v: View) : RecyclerView.ViewHolder(v) {
            private val playerView: androidx.media3.ui.PlayerView = v.findViewById(R.id.playerView)
            private val textCard: View = v.findViewById(R.id.textCard)
            private val title: TextView = v.findViewById(R.id.cardTitle)
            private val body: TextView = v.findViewById(R.id.cardBody)

            fun bind(item: ContentItem, isActive: Boolean) {
                playerView.visibility = View.GONE
                textCard.visibility = View.VISIBLE
                title.text = item.title
                body.text = item.body ?: item.link ?: ""
            }
        }

        inner class VideoVH(v: View) : RecyclerView.ViewHolder(v) {
            private val playerView: androidx.media3.ui.PlayerView = v.findViewById(R.id.playerView)
            private val textCard: View = v.findViewById(R.id.textCard)

            fun bind(item: ContentItem, isActive: Boolean) {
                textCard.visibility = View.GONE
                playerView.visibility = View.VISIBLE

                val pos = bindingAdapterPosition
                var p = players[pos]
                if (p == null) {
                    p = ExoPlayer.Builder(playerView.context).build().also { exo ->
                        exo.repeatMode = Player.REPEAT_MODE_ALL
                        playerView.player = exo
                        playerView.controllerShowTimeoutMs = 0
                        playerView.hideController()
                        if (!item.mediaUri.isNullOrBlank()) {
                            exo.setMediaItem(MediaItem.fromUri(item.mediaUri!!))
                            exo.prepare()
                        }
                    }
                    players[pos] = p
                }
                p.playWhenReady = isActive
            }
        }
    }
}

/** Local content (safe). Adjust if you want different mixes for GOOD vs OKAY. */
object LocalContentRepository {
    fun itemsFor(category: Category, mood: Mood): List<ContentItem> {
        val base = when (category) {
            Category.EDUCATE -> educate
            Category.LAUGH   -> laugh
            Category.MOTIVATE-> motivate
        }
        return when (mood) {
            Mood.LOW, Mood.MEH -> base.shuffled().take(12)
            Mood.OKAY, Mood.GOOD -> base.shuffled().take(16)
            Mood.GREAT -> base.shuffled().take(18)
        }
    }

    private val educate = listOf(
        ContentItem("e1", Category.EDUCATE, "text", "What is CBT?",
            "CBT builds skills to reframe unhelpful thoughts."),
        ContentItem("e2", Category.EDUCATE, "text", "Sleep tip",
            "Try a 10-minute wind-down and screens-off before bed."),
        ContentItem("e3", Category.EDUCATE, "text", "Hydration boost",
            "Drink a glass of water during this session.")
        // Example video (add a real URI to test):
        // ContentItem("ev1", Category.EDUCATE, "video", "Breathing Demo", mediaUri="file:///sdcard/Movies/demo.mp4")
    )

    private val laugh = listOf(
        ContentItem("l1", Category.LAUGH, "text", "Two-liner",
            "Why did the computer get cold? It forgot to close Windows."),
        ContentItem("l2", Category.LAUGH, "text", "Tiny chuckle",
            "Parallel lines have so much in common. It’s a shame they’ll never meet.")
    )

    private val motivate = listOf(
        ContentItem("m1", Category.MOTIVATE, "text", "Micro-goal",
            "Stand up and stretch right now. 30 seconds counts."),
        ContentItem("m2", Category.MOTIVATE, "text", "Walk break",
            "Take a 2-minute walk and notice 5 sounds around you.")
    )
}
