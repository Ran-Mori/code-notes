package com.cbservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi


class TrackService: Service() {

    private var locationManager: LocationManager? = null
    private var provider: String? = null
    private val criteria = Criteria().apply {
        accuracy = Criteria.ACCURACY_COARSE
        isCostAllowed = false
    }
    private val listener = LocationListener {
        Log.d("IzumiSakai", "LocationListener result -> $it")
    }

    private val handler = Handler(Looper.getMainLooper())

    private val runnable: Runnable = object : Runnable {
        override fun run() {
            Log.d("IzumiSakai", "handler runnable run")
            //post the same runnable with 1 sec delayed
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("IzumiSakai", "service onStartCommand call")
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        provider = locationManager?.getBestProvider(criteria, false)

        handler.postDelayed(runnable, 1000)

        try {
            val location = locationManager?.getLastKnownLocation(provider ?: "")
            location?.let { listener.onLocationChanged(it) }
        } catch (e :SecurityException) {

        } catch (t: Throwable) {

        } catch (e: Exception) {

        }
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("IzumiSakai", "service onUnbind call")
        handler.removeCallbacks(runnable)
        return super.onUnbind(intent)
    }
}