package com.example.cameratoggle

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Приёмник, необходимый системе для регистрации приложения
 * как администратора устройства (Device Admin).
 * Сам по себе не выполняет никаких действий, кроме уведомлений
 * о включении/отключении админ-прав.
 */
class CameraAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Права администратора активированы", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Права администратора отключены", Toast.LENGTH_SHORT).show()
    }
}
