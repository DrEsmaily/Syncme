package io.element.android.libraries.matrix.api.diagnostics

import android.content.Context

/**
 * User-controlled, persisted diagnostic switches. Both are OFF on a fresh
 * installation. Changes apply to dedicated SyncMe audit instrumentation;
 * native Rust tracing verbosity is fixed when the SDK starts, so it needs
 * an app process restart after toggling.
 */
object SyncMeDiagnosticsControl {
    private const val PREFS = "syncme-diagnostics-settings-v1"
    private const val STARTUP = "startup_enabled"
    private const val TLS = "tls_enabled"
    private const val DECRYPTION = "decryption_enabled"

    fun decryptionEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(DECRYPTION, false)

    fun setDecryptionEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(DECRYPTION, enabled).apply()
        DecryptionDiagnostics.configure(context, enabled)
    }

    fun startupEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(STARTUP, false)

    fun tlsEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(TLS, false)

    fun setStartupEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(STARTUP, enabled).apply()
        StartupTrace.setEnabled(context.applicationContext, enabled)
    }

    fun setTlsEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(TLS, enabled).apply()
        TlsAudit.setEnabled(context.applicationContext, enabled)
    }
}
