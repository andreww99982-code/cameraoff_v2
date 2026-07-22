package com.example.cameratoggle

import android.os.Build

object DeviceInstructions {

    data class Info(val vendorLabel: String, val steps: String)

    fun forCurrentDevice(): Info {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            brand.contains("redmi") || brand.contains("poco") -> Info(
                vendorLabel = "Xiaomi / Redmi / POCO (MIUI / HyperOS)",
                steps = """
                    1. Настройки → Приложения → Управление приложениями → Privacy Switch → Автозапуск — включить.
                    2. Там же → Экономия батареи → выбрать «Без ограничений».
                    3. Настройки → Дополнительные настройки → Для разработчиков (если скрыто — 7 раз нажать на «Версия MIUI» в «О телефоне») → включить «Отключить оптимизацию MIUI» → перезагрузить телефон.
                    4. После перезагрузки — деактивировать и заново активировать права администратора в этом приложении.
                """.trimIndent()
            )

            manufacturer.contains("honor") || brand.contains("honor") -> Info(
                vendorLabel = "Honor (MagicOS)",
                steps = """
                    1. Настройки → Приложения → Privacy Switch → Батарея → выбрать «Без ограничений» (не «Управляется автоматически»).
                    2. Настройки → Батарея → Запуск приложений → найти Privacy Switch → переключить в ручной режим → включить «Автозапуск», «Вторичный запуск» и «Работа в фоне».
                    3. Настройки → Безопасность → Дополнительно → Администраторы устройства → убедиться, что Privacy Switch активен.
                    4. Перезагрузить устройство и повторить активацию прав в приложении.
                """.trimIndent()
            )

            manufacturer.contains("realme") || brand.contains("realme") ||
            manufacturer.contains("oppo") || brand.contains("oppo") -> Info(
                vendorLabel = "Realme / OPPO (Realme UI / ColorOS)",
                steps = """
                    1. Настройки → Управление приложениями → Privacy Switch → Автозапуск — включить.
                    2. Настройки → Батарея → Энергосбережение приложений → Privacy Switch → выбрать «Разрешить работу в фоне» / «Без ограничений».
                    3. Настройки → Безопасность → Дополнительные настройки безопасности → Администраторы устройства → проверить, что Privacy Switch включён.
                    4. Перезагрузить устройство и заново активировать права в приложении.
                """.trimIndent()
            )

            manufacturer.contains("samsung") || brand.contains("samsung") -> Info(
                vendorLabel = "Samsung (One UI)",
                steps = """
                    1. Настройки → Приложения → Privacy Switch → Батарея → выбрать «Не ограничивать».
                    2. Настройки → Батарея и обслуживание устройства → Батарея → Ограничения фоновой активности → убрать Privacy Switch из «Спящих» и «Глубокий сон».
                    3. Настройки → Биометрия и безопасность → Другие параметры безопасности → Администраторы устройства → проверить, что Privacy Switch активен.
                """.trimIndent()
            )

            manufacturer.contains("huawei") || brand.contains("huawei") -> Info(
                vendorLabel = "Huawei (EMUI / HarmonyOS)",
                steps = """
                    1. Настройки → Приложения → Privacy Switch → Батарея → «Запуск приложения» → переключить в ручной режим → включить все три пункта (автозапуск, вторичный запуск, работа в фоне).
                    2. Настройки → Безопасность → Другие настройки безопасности → Администраторы устройства → проверить активность Privacy Switch.
                """.trimIndent()
            )

            manufacturer.contains("vivo") || brand.contains("vivo") -> Info(
                vendorLabel = "Vivo (Funtouch / OriginOS)",
                steps = """
                    1. Настройки → Батарея → Фоновое энергопотребление → Privacy Switch → «Разрешить фоновую активность».
                    2. Настройки → Приложения → Управление автозапуском → включить для Privacy Switch.
                    3. Настройки → Пароли и безопасность → Администраторы устройства → проверить, что Privacy Switch активен.
                """.trimIndent()
            )

            else -> Info(
                vendorLabel = "${Build.MANUFACTURER} ${Build.MODEL}",
                steps = """
                    Модель не распознана как одна из известных сильно кастомизированных прошивок.
                    Общие шаги: Настройки → Приложения → Privacy Switch → Батарея → «Без ограничений» / «Не оптимизировать»,
                    затем Настройки → Безопасность → Администраторы устройства → проверить, что Privacy Switch активен.
                """.trimIndent()
            )
        }
    }

    fun deviceSummary(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
}
