package com.example.moodrise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var btnLaugh: Button
    private lateinit var btnLearn: Button
    private lateinit var btnMotivate: Button
    private lateinit var sectionHeader: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLaugh = findViewById(R.id.btnLaugh)
        btnLearn = findViewById(R.id.btnLearn)
        btnMotivate = findViewById(R.id.btnMotivate)
        sectionHeader = findViewById(R.id.sectionHeader)
        bottomNav = findViewById(R.id.bottomNav)

        btnLaugh.setOnClickListener { openFeed("laughs") }
        btnLearn.setOnClickListener { openFeed("learn") }
        btnMotivate.setOnClickListener { openFeed("motivate") }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> true
                R.id.nav_upload -> { Toast.makeText(this,"Upload tapped",Toast.LENGTH_SHORT).show(); true }
                R.id.nav_profile -> { Toast.makeText(this,"Profile tapped",Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
    }

    private fun openFeed(topic: String) {
        val i = Intent(this, FeedActivity::class.java)
        i.putExtra(FeedActivity.EXTRA_TOPIC, topic)
        startActivity(i)
    }
}
