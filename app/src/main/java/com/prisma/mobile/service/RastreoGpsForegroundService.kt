package com.prisma.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.prisma.mobile.R
import com.prisma.mobile.ui.conductor.ConductorMapaRutaActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RastreoGpsForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isTracking = false

    companion object {
        const val CHANNEL_ID = "PRISMA_GPS_TRACKING_CHANNEL"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val EXTRA_RUTA_ID = "EXTRA_RUTA_ID"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val rutaId = intent.getLongExtra(EXTRA_RUTA_ID, 1L)
                startForegroundTracking(rutaId)
            }
            ACTION_STOP -> {
                stopForegroundTracking()
            }
        }
        return START_STICKY
    }

    private fun startForegroundTracking(rutaId: Long) {
        if (isTracking) return
        isTracking = true

        val notification = buildNotification("Ruta #$rutaId en reparto", "Transmitiendo telemetría satelital en segundo plano...")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            while (isActive && isTracking) {
                delay(10000)
            }
        }
    }

    private fun stopForegroundTracking() {
        isTracking = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = Intent(this, ConductorMapaRutaActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PRISMA Logística: $title")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo de Flota en Tiempo Real",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente para telemetría continua de camiones de reparto PRISMA"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
