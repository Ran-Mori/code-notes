package com.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private val textView: TextView by lazy { findViewById(R.id.text_view) }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView.setOnClickListener {
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val result = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
            if (result) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 3)
            }
        } else {
            //makeSystemNotification()
            makeCustomNotification()
        }
    }

    private fun makeSystemNotification() {
        val channel = NotificationChannel(
            "123456",
            "notification_channel_example",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.createNotificationChannel(channel)

        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:13786542678"))
        val notification = Notification.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentText("hello notification")
            .setWhen(System.currentTimeMillis())
            .setFlag(Notification.FLAG_AUTO_CANCEL, true)
            .setContentIntent(PendingIntent.getActivities(
                this,
                0, arrayOf(intent),
                PendingIntent.FLAG_UPDATE_CURRENT xor PendingIntent.FLAG_IMMUTABLE
            ))
            .build()
        manager?.notify(1, notification)
    }

    private fun makeCustomNotification() {
        val channel = NotificationChannel(
            "123456",
            "notification_channel_example",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.createNotificationChannel(channel)

        val remoteView = RemoteViews(packageName, R.layout.remote_view).apply {
            setTextViewText(R.id.remote_text, "remote_text")
            setImageViewResource(R.id.remote_image, R.drawable.ic_launcher_background)
        }

        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:13786542678"))
        val notification = Notification.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setFlag(Notification.FLAG_AUTO_CANCEL, true)
            .setCustomContentView(remoteView)
            .setContentIntent(PendingIntent.getActivities(
                this,
                0, arrayOf(intent),
                PendingIntent.FLAG_UPDATE_CURRENT xor PendingIntent.FLAG_IMMUTABLE
            ))
            .build()
        manager?.notify(1, notification)
    }
}