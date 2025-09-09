package com.example.moodrise.wellness

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.moodrise.MainActivity
import kotlin.random.Random

object WellnessTips {
    private val tips = listOf(
        "60-second breath: inhale 4, exhale 6 — repeat 10x.",
        "Quick win: drink a glass of water now.",
        "Stand and stretch your shoulders for 30 seconds.",
        "Text a friend something you appreciate about them.",
        "Take a 2-minute walk and count 5 green things you see.",
        "Write one sentence about how you feel right now."
    )
    fun random() = tips.random()
}

object Notifier {
    const val CHANNEL_ID = "moodrise_tips"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "Wellness Tips", NotificationManager.IMPORTANCE_DEFAULT)
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    fun send(ctx: Context, message: String) {
        ensureChannel(ctx)
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, 10, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notif = Notification.Builder(ctx, CHANNEL_ID)
            .setContentTitle("Wellness nudges")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(Random.nextInt(), notif)
    }
}
