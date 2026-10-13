package io.element.android.libraries.matrix.api.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-local foreground hint for Matrix presence.
 * null means no Activity lifecycle transition has been observed yet.
 */
object SyncMeAppVisibility {
    private val mutableIsForeground = MutableStateFlow<Boolean?>(null)
    val isForeground: StateFlow<Boolean?> = mutableIsForeground

    fun setForeground(value: Boolean) {
        if (mutableIsForeground.value != value) {
            io.element.android.libraries.matrix.api.diagnostics.DecryptionDiagnostics.record(
                if (value) {
                    io.element.android.libraries.matrix.api.diagnostics.DecryptionDiagnostics.Stage.FOREGROUND
                } else {
                    io.element.android.libraries.matrix.api.diagnostics.DecryptionDiagnostics.Stage.BACKGROUND
                },
            )
        }
        mutableIsForeground.value = value
    }
}
