package com.widget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews


class CurrentMoodWidgetProvider: AppWidgetProvider() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "CurrentMoodWidgetProvider onUpdate call")
        val widgetSize = appWidgetIds.size
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (i in 0 until widgetSize) {
            val appWidgetId = appWidgetIds[i]
            Log.d(TAG, "CurrentMoodWidgetProvider updating widget[id] $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.widgetlayout)
            val intent = Intent(context, CurrentMoodService::class.java)
            intent.action = CurrentMoodService.UPDATE_MOOD
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingIntent = PendingIntent.getService(context, 0, intent, FLAG_MUTABLE)
            views.setOnClickPendingIntent(R.id.widget_text_view, pendingIntent)
            Log.d(TAG, "CurrentMoodWidgetProvider pending intent set")

            // Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}