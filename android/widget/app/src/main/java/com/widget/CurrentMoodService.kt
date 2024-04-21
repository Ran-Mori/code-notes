package com.widget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import java.util.*


class CurrentMoodService : Service() {

    private var currentMood: String? = null
    private var moods: LinkedList<String> = LinkedList()
    private val randomMood: String
        get() {
            val r = Random(Calendar.getInstance().timeInMillis)
            val pos: Int = r.nextInt(moods.size)
            return moods[pos]
        }

    companion object {
        const val UPDATE_MOOD = "UpdateMood"
        const val CURRENT_MOOD = "CurrentMood"
    }

    init {
        moods = LinkedList()
        fillMoodsList()
    }

    private fun fillMoodsList() {
        moods.add(":)")
        moods.add(":(")
        moods.add(":D")
        moods.add(":X")
        moods.add(":S")
        moods.add(";)")
        currentMood = ";)"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(CurrentMoodWidgetProvider.TAG, "CurrentMoodService onStartCommand call")
        updateMood(intent)
        stopSelf(startId)
        return START_STICKY
    }

    private fun updateMood(intent: Intent?) {
        Log.d(CurrentMoodWidgetProvider.TAG, "CurrentMoodService updateMood call")
        intent ?: return

        val requestedAction = intent.action
        Log.d(CurrentMoodWidgetProvider.TAG, "CurrentMoodService This is the action $requestedAction")
        if (requestedAction != null && requestedAction == UPDATE_MOOD) {
            currentMood = randomMood
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
            Log.d(CurrentMoodWidgetProvider.TAG, "CurrentMoodService This is the currentMood $currentMood to widget $widgetId")
            val appWidgetMan = AppWidgetManager.getInstance(this)
            val views = RemoteViews(packageName, R.layout.widgetlayout)
            views.setTextViewText(R.id.widget_text_view, currentMood)
            appWidgetMan.updateAppWidget(widgetId, views)
            Log.d(CurrentMoodWidgetProvider.TAG, "CurrentMoodService CurrentMood updated!")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}