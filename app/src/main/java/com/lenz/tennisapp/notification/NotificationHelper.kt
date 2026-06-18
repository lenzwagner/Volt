package com.lenz.tennisapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lenz.tennisapp.MainActivity
import com.lenz.tennisapp.R

object NotificationHelper {

    const val CHANNEL_API_KEY = "api_key_alerts"
    const val CHANNEL_LIVE = "live_matches"

    const val NOTIF_TENNIS_KEY_EXPIRED = 1001
    const val NOTIF_ODDS_KEY_EXPIRED = 1002
    const val NOTIF_ODDS_LOW = 1003

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_API_KEY,
                "API-Key Warnungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen wenn ein API-Key abgelaufen ist"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LIVE,
                "Live Matches",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen zu Live-Spielen"
            }
        )
    }

    fun notifyTennisKeyExpired(context: Context) {
        showNotification(
            context = context,
            id = NOTIF_TENNIS_KEY_EXPIRED,
            title = "API-Tennis Key abgelaufen",
            text = "Bitte einen neuen Key in den Einstellungen eintragen. Neuen Key unter api-tennis.com holen.",
            channel = CHANNEL_API_KEY
        )
    }

    fun notifyOddsKeyExpired(context: Context) {
        showNotification(
            context = context,
            id = NOTIF_ODDS_KEY_EXPIRED,
            title = "Odds API Key erschöpft",
            text = "Das monatliche Kontingent (500 Requests) ist aufgebraucht. Neuen Key unter the-odds-api.com holen.",
            channel = CHANNEL_API_KEY
        )
    }

    fun notifyOddsLow(context: Context, remaining: Int) {
        showNotification(
            context = context,
            id = NOTIF_ODDS_LOW,
            title = "Odds API: Nur noch $remaining Requests",
            text = "Das monatliche Kontingent ist fast aufgebraucht.",
            channel = CHANNEL_API_KEY
        )
    }

    fun notifyMatchStarted(context: Context, matchId: String, title: String, text: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "match_detail")
            putExtra("matchId", matchId)
        }
        val id = matchId.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_tennis_ball)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        text: String,
        channel: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "settings")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_tennis_ball)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted
        }
    }
}
