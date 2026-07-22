package com.example.cameratoggle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * ВАЖНО: этот сервис НЕ блокирует доступ к камере. Он только показывает
 * чёрный экран поверх интерфейса, когда обнаруживает, что какое-то
 * приложение начало использовать камеру. Настоящая картинка с сенсора
 * при этом всё равно передаётся приложению, которое запросило камеру —
 * это визуальная защита/индикатор, а не блокировка доступа.
 */
class CameraGuardService : Service() {

    private lateinit var cameraManager: CameraManager
    private lateinit var windowManager: WindowManager
    private var overlayView: LinearLayout? = null

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            showOverlay()
        }

        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            hideOverlay()
        }
    }

    companion object {
        const val CHANNEL_ID = "camera_guard_channel"
        const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.example.cameratoggle.action.STOP_GUARD"
    }

    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        cameraManager.registerAvailabilityCallback(availabilityCallback, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            cameraManager.unregisterAvailabilityCallback(availabilityCallback)
        } catch (_: Exception) {
        }
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (overlayView != null) return
        try {
            val layout = LinearLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            val warning = TextView(this).apply {
                text = "⚠ Обнаружено использование камеры\nЭто визуальный оверлей, доступ к камере он не блокирует"
                setTextColor(Color.WHITE)
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
            }
            layout.addView(warning)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE
            )
            windowManager.addView(layout, params)
            overlayView = layout
        } catch (_: Exception) {
            // Нет разрешения "Отображение поверх других приложений" — молча пропускаем,
            // статус разрешения проверяется и показывается в самом приложении.
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Мониторинг камеры",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, CameraGuardService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Экран приватности активен")
            .setContentText("Следим за использованием камеры")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .addAction(0, "Остановить", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
}
