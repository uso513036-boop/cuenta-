package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object SystemDualAppsLauncher {

    /**
     * Attempts to open the device manufacturer's native Dual Apps / App Cloner settings.
     * Supported: Xiaomi/Redmi/POCO (MIUI / HyperOS), Samsung (Dual Messenger), Oppo/Realme, Vivo, Huawei/Honor, OnePlus.
     */
    fun openDeviceDualAppSettings(context: Context): Boolean {
        val intents = listOf(
            // Xiaomi / MIUI / HyperOS (Dual Apps / Aplicaciones Duales)
            Intent().setComponent(ComponentName("com.miui.securityadd", "com.miui.securityadd.dualapp.ui.DualAppActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycore", "com.miui.securitycore.ui.DualAppActivity")),
            Intent("miui.intent.action.DUAL_APP"),

            // Samsung Dual Messenger / Secure Folder
            Intent().setComponent(ComponentName("com.samsung.android.da.daagent", "com.samsung.android.da.daagent.MainActivity")),
            Intent().setComponent(ComponentName("com.samsung.knox.securefolder", "com.samsung.knox.securefolder.presentation.switcher.SwitchActivity")),

            // Oppo / Realme / ColorOS (App Cloner / Clonador de Aplicaciones)
            Intent().setComponent(ComponentName("com.coloros.clone", "com.coloros.clone.CloneSettingsActivity")),
            Intent().setComponent(ComponentName("com.oplus.clone", "com.oplus.clone.CloneSettingsActivity")),

            // Vivo / FuntouchOS (App Clone)
            Intent().setComponent(ComponentName("com.vivo.doubleinstance", "com.vivo.doubleinstance.DoubleInstanceActivity")),

            // Huawei / Honor (App Twin)
            Intent().setComponent(ComponentName("com.huawei.appclone", "com.huawei.appclone.AppCloneSettingsActivity")),

            // OnePlus (Parallel Apps)
            Intent().setComponent(ComponentName("com.oneplus.clone", "com.oneplus.clone.CloneMainActivity")),

            // Fallback: System Multiple Users / Managed Profile / All Apps Settings
            Intent(Settings.ACTION_SYNC_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_SETTINGS),
            Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "Abriendo configuración de Aplicaciones Duales del teléfono...", Toast.LENGTH_SHORT).show()
                return true
            } catch (e: Exception) {
                // Try next intent
            }
        }

        // Ultimate fallback: App settings
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            Toast.makeText(context, "Abre 'Aplicaciones Duales' o 'Clonador de Apps' en Ajustes.", Toast.LENGTH_LONG).show()
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir Ajustes del sistema.", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * Launches the native installed application directly.
     */
    fun launchNativeApp(context: Context, packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                Toast.makeText(context, "No se encontró la aplicación instalada ($packageName)", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir la app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
