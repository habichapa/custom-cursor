package com.whatswithai.customcursor

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service that:
 *  1. Keeps the process alive even when the user leaves the app
 *  2. Adds / removes the CursorOverlayView via WindowManager
 *
 * Lifecycle:
 *   startService(ACTION_START) → adds overlay
 *   startService(ACTION_STOP)  → removes overlay, stops self
 */
class CursorService : Service() {

    companion object {
        const val ACTION_START = "START_CURSOR"
        const val ACTION_STOP  = "STOP_CURSOR"
        private const val NOTIF_CHANNEL = "cursor_channel"
        private const val NOTIF_ID      = 1001

        /** Simple flag so MainActivity can read running state. */
        var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: CursorOverlayView? = null

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification())
                addOverlay()
                isRunning = true
            }
            ACTION_STOP  -> {
                removeOverlay()
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Overlay management ────────────────────────────────────────────────────

    private fun addOverlay() {
        if (overlayView != null) return   // already added

        val view = CursorOverlayView(this)
        overlayView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // NOT_FOCUSABLE  → no keyboard stealing
            // NOT_TOUCH_MODAL → pointer events pass through to apps below
            //                   HOVER events are still received because hover
            //                   dispatch uses the topmost window regardless of
            //                   NOT_TOUCH_MODAL.
            // LAYOUT_IN_SCREEN → fill entire screen incl. status bar area
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(view, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL,
                "Custom Cursor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Cursor overlay is active"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, CursorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_cursor_arrow)
            .setContentTitle("Custom Cursor active")
            .setContentText("Mouse cursor overlay is running")
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
