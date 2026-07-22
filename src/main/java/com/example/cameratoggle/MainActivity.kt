package com.example.cameratoggle

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File

class MainActivity : AppCompatActivity() {

    // ---- Камера (device admin) ----
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private lateinit var tvAdminStatus: TextView
    private lateinit var tvCameraStatus: TextView
    private lateinit var btnAdmin: Button
    private lateinit var switchCamera: SwitchMaterial
    private var suppressCameraListener = false

    // ---- Микрофон (мягкий mute) ----
    private lateinit var audioManager: AudioManager
    private lateinit var tvMicStatus: TextView
    private lateinit var switchMic: SwitchMaterial
    private var suppressMicListener = false

    // ---- Датчики (ярлык на системные настройки) ----
    private lateinit var btnSensors: Button

    // ---- Экран приватности (чёрный оверлей) ----
    private lateinit var switchOverlay: SwitchMaterial
    private lateinit var tvOverlayPermissionStatus: TextView
    private lateinit var btnOverlayPermission: Button
    private var suppressOverlayListener = false

    // ---- Диагностика устройства ----
    private lateinit var tvDeviceSummary: TextView
    private lateinit var tvVendorLabel: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvAdminDebug: TextView
    private lateinit var btnOpenAppSettings: Button
    private lateinit var btnIgnoreBattery: Button

    companion object {
        private const val REQUEST_ADMIN_ACTIVATION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, CameraAdminReceiver::class.java)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        tvAdminStatus = findViewById(R.id.tvAdminStatus)
        tvCameraStatus = findViewById(R.id.tvCameraStatus)
        btnAdmin = findViewById(R.id.btnAdmin)
        switchCamera = findViewById(R.id.switchCamera)

        tvMicStatus = findViewById(R.id.tvMicStatus)
        switchMic = findViewById(R.id.switchMic)

        btnSensors = findViewById(R.id.btnSensors)

        switchOverlay = findViewById(R.id.switchOverlay)
        tvOverlayPermissionStatus = findViewById(R.id.tvOverlayPermissionStatus)
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission)

        tvDeviceSummary = findViewById(R.id.tvDeviceSummary)
        tvVendorLabel = findViewById(R.id.tvVendorLabel)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvAdminDebug = findViewById(R.id.tvAdminDebug)
        btnOpenAppSettings = findViewById(R.id.btnOpenAppSettings)
        btnIgnoreBattery = findViewById(R.id.btnIgnoreBattery)

        setupDeviceDiagnostics()

        btnAdmin.setOnClickListener { onAdminButtonClicked() }

        switchCamera.setOnCheckedChangeListener { _, isChecked ->
            if (suppressCameraListener) return@setOnCheckedChangeListener
            onCameraSwitchToggled(isChecked)
        }

        switchMic.setOnCheckedChangeListener { _, isChecked ->
            if (suppressMicListener) return@setOnCheckedChangeListener
            onMicSwitchToggled(isChecked)
        }

        btnSensors.setOnClickListener { openSensorsSettings() }
        btnOpenAppSettings.setOnClickListener { openAppSettings() }
        btnIgnoreBattery.setOnClickListener { requestIgnoreBatteryOptimizations() }

        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (suppressOverlayListener) return@setOnCheckedChangeListener
            onOverlaySwitchToggled(isChecked)
        }
        btnOverlayPermission.setOnClickListener { requestOverlayPermission() }

        showCrashLogIfAny()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    // ==================== ДИАГНОСТИКА УСТРОЙСТВА ====================

    private fun setupDeviceDiagnostics() {
        tvDeviceSummary.text = DeviceInstructions.deviceSummary()
        val info = DeviceInstructions.forCurrentDevice()
        tvVendorLabel.text = "Определено как: ${info.vendorLabel}"
        tvInstructions.text = info.steps
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть настройки приложения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "Ограничения батареи уже отключены для этого приложения", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть запрос: ${e.message}. Откройте настройки батареи вручную.", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== ЖУРНАЛ СБОЕВ (для диагностики) ====================

    private fun showCrashLogIfAny() {
        val logFile = File(filesDir, "crash_log.txt")
        if (logFile.exists()) {
            val text = try { logFile.readText() } catch (e: Exception) { "Не удалось прочитать журнал: ${e.message}" }
            AlertDialog.Builder(this)
                .setTitle("Приложение недавно завершилось с ошибкой")
                .setMessage(text)
                .setPositiveButton("Понятно") { d, _ ->
                    logFile.delete()
                    d.dismiss()
                }
                .setNeutralButton("Скопировать текст") { d, _ ->
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("crash_log", text))
                    Toast.makeText(this, "Текст ошибки скопирован", Toast.LENGTH_SHORT).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    // ==================== КАМЕРА ====================

    private fun onCameraSwitchToggled(isChecked: Boolean) {
        if (!isAdminActive()) return
        try {
            devicePolicyManager.setCameraDisabled(adminComponent, !isChecked)
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Система запретила изменить состояние камеры: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при переключении камеры: ${e.message}", Toast.LENGTH_LONG).show()
        }
        updateCameraStatusText()
    }

    private fun onAdminButtonClicked() {
        // Всегда перепроверяем состояние заново перед решением, что делать —
        // чтобы кнопка не «залипала» на одном действии.
        val currentlyActive = isAdminActive()
        if (currentlyActive) {
            devicePolicyManager.removeActiveAdmin(adminComponent)
            Toast.makeText(this, "Права администратора отозваны", Toast.LENGTH_SHORT).show()
            refreshUi()
        } else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Это разрешение нужно, чтобы приложение могло включать и выключать камеру устройства."
                )
            }
            try {
                startActivityForResult(intent, REQUEST_ADMIN_ACTIVATION)
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть экран активации: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADMIN_ACTIVATION) {
            if (isAdminActive()) {
                Toast.makeText(this, "Готово, права администратора получены", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Права администратора не были предоставлены", Toast.LENGTH_SHORT).show()
            }
            refreshUi()
        }
    }

    /**
     * Двойная проверка: основной метод isAdminActive() плюс поиск нашего
     * компонента в полном списке активных админов. На некоторых прошивках
     * первый метод иногда врёт из-за кастомных обёрток над DevicePolicyManager —
     * второй способ более надёжен как перекрёстная проверка.
     */
    private fun isAdminActive(): Boolean {
        val viaDirectCall = try {
            devicePolicyManager.isAdminActive(adminComponent)
        } catch (e: Exception) {
            null
        }
        val viaList = try {
            devicePolicyManager.activeAdmins?.any { it == adminComponent } ?: false
        } catch (e: Exception) {
            null
        }
        tvAdminDebug.text = "debug: isAdminActive()=$viaDirectCall, найден в activeAdmins=$viaList"
        return (viaDirectCall == true) || (viaList == true)
    }

    private fun refreshUi() {
        val adminActive = isAdminActive()

        tvAdminStatus.text = if (adminActive) {
            "Права администратора: активны"
        } else {
            getString(R.string.status_no_admin)
        }

        btnAdmin.text = if (adminActive) {
            getString(R.string.btn_deactivate_admin)
        } else {
            getString(R.string.btn_activate_admin)
        }

        switchCamera.isEnabled = adminActive
        updateCameraStatusText()

        val micMuted = try {
            @Suppress("DEPRECATION")
            audioManager.isMicrophoneMute
        } catch (e: Exception) {
            false
        }
        suppressMicListener = true
        switchMic.isChecked = !micMuted
        suppressMicListener = false
        updateMicStatusText(!micMuted)

        refreshOverlayUi()
    }

    // ==================== ЭКРАН ПРИВАТНОСТИ (ЧЁРНЫЙ ОВЕРЛЕЙ) ====================

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun refreshOverlayUi() {
        val granted = hasOverlayPermission()
        tvOverlayPermissionStatus.text = if (granted) {
            "Разрешение на отображение поверх интерфейса: предоставлено"
        } else {
            "Разрешение на отображение поверх интерфейса: НЕ предоставлено (нужно для оверлея)"
        }
        btnOverlayPermission.isEnabled = !granted
        suppressOverlayListener = true
        if (!granted) {
            switchOverlay.isChecked = false
        }
        switchOverlay.isEnabled = granted
        suppressOverlayListener = false
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun onOverlaySwitchToggled(isChecked: Boolean) {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Сначала выдайте разрешение на отображение поверх интерфейса", Toast.LENGTH_SHORT).show()
            suppressOverlayListener = true
            switchOverlay.isChecked = false
            suppressOverlayListener = false
            return
        }
        val serviceIntent = Intent(this, CameraGuardService::class.java)
        if (isChecked) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2001)
            }
            try {
                startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось запустить мониторинг: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            stopService(serviceIntent)
        }
    }

    private fun updateCameraStatusText() {
        if (!isAdminActive()) {
            tvCameraStatus.text = "Управление недоступно без прав администратора"
            suppressCameraListener = true
            switchCamera.isChecked = true
            suppressCameraListener = false
            return
        }
        val disabled = try {
            devicePolicyManager.getCameraDisabled(adminComponent)
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось прочитать состояние камеры: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
        suppressCameraListener = true
        switchCamera.isChecked = !disabled
        suppressCameraListener = false
        tvCameraStatus.text = if (disabled) {
            getString(R.string.status_disabled)
        } else {
            getString(R.string.status_enabled)
        }
    }

    // ==================== МИКРОФОН ====================

    private fun onMicSwitchToggled(isChecked: Boolean) {
        try {
            @Suppress("DEPRECATION")
            audioManager.isMicrophoneMute = !isChecked
        } catch (e: SecurityException) {
            Toast.makeText(this, "Система не разрешила изменить состояние микрофона", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка микрофона: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        updateMicStatusText(isChecked)
    }

    private fun updateMicStatusText(enabled: Boolean) {
        tvMicStatus.text = if (enabled) {
            getString(R.string.mic_active)
        } else {
            getString(R.string.mic_muted)
        }
    }

    // ==================== ДАТЧИКИ ====================

    private fun openSensorsSettings() {
        try {
            val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                Toast.makeText(
                    this,
                    "Найдите раздел «Конфиденциальность» → «Датчики выкл.» вручную",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть настройки: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
