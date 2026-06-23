package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.service.PlaybackManager

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_PLAY_PAUSE) {
            val isPlaying = PlaybackManager.isPlaying.value
            if (isPlaying) {
                PlaybackManager.pause()
            } else {
                PlaybackManager.resume()
            }
            updateAllWidgets(context)
        } else if (action == ACTION_PREV) {
            PlaybackManager.previous()
            updateAllWidgets(context)
        } else if (action == ACTION_NEXT) {
            PlaybackManager.next()
            updateAllWidgets(context)
        } else if (action == ACTION_UPDATE_STATE) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.example.widget.ACTION_PREV"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_UPDATE_STATE = "com.example.widget.ACTION_UPDATE_STATE"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MusicWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.music_widget_layout)

            val currentSong = PlaybackManager.currentSong.value
            val isPlaying = PlaybackManager.isPlaying.value

            if (currentSong != null) {
                views.setTextViewText(R.id.widget_title, currentSong.title)
                views.setTextViewText(R.id.widget_artist, currentSong.artist)
            } else {
                views.setTextViewText(R.id.widget_title, "Melodix Music Player")
                views.setTextViewText(R.id.widget_artist, "No Song Playing")
            }

            if (isPlaying) {
                views.setImageViewResource(R.id.widget_btn_play_pause, R.drawable.ic_widget_pause)
            } else {
                views.setImageViewResource(R.id.widget_btn_play_pause, R.drawable.ic_widget_play)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            // Play/Pause Action
            val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(context, 1, playPauseIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

            // Previous Action
            val prevIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_PREV
            }
            val prevPendingIntent = PendingIntent.getBroadcast(context, 2, prevIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            // Next Action
            val nextIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(context, 3, nextIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            // Click root layout to open the MainActivity
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val launchPendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
