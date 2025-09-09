package com.example.moodrise

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FeedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOPIC = "EXTRA_TOPIC" // "laughs", "learn", "motivate"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        val backBtn: ImageButton = findViewById(R.id.backBtn)
        val topicTitle: TextView = findViewById(R.id.topicTitle)

        val topic = intent.getStringExtra(EXTRA_TOPIC) ?: "laughs"
        topicTitle.text = when (topic) {
            "learn" -> "Learn Feed"
            "motivate" -> "Motivate Feed"
            else -> "Laugh Feed"
        }

        backBtn.setOnClickListener { finish() }
    }
}
