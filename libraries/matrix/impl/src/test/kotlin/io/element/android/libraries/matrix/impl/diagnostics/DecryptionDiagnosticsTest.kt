package io.element.android.libraries.matrix.impl.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.diagnostics.DecryptionDiagnostics
import io.element.android.libraries.matrix.api.diagnostics.SyncMeDiagnosticsControl
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DecryptionDiagnosticsTest : RobolectricTest() {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.getSharedPreferences("syncme-diagnostics-settings-v1", Context.MODE_PRIVATE).edit().clear().commit()
        DecryptionDiagnostics.configure(context, false)
        DecryptionDiagnostics.clear()
    }

    @After
    fun tearDown() {
        DecryptionDiagnostics.configure(context, false)
        DecryptionDiagnostics.clear()
    }

    @Test
    fun `default disabled records nothing and enabling applies without restart`() {
        assertThat(SyncMeDiagnosticsControl.decryptionEnabled(context)).isFalse()
        DecryptionDiagnostics.record(DecryptionDiagnostics.Stage.SEND_REQUEST)
        assertThat(DecryptionDiagnostics.report()).doesNotContain("SEND_REQUEST")
        SyncMeDiagnosticsControl.setDecryptionEnabled(context, true)
        DecryptionDiagnostics.record(DecryptionDiagnostics.Stage.SEND_REQUEST)
        assertThat(DecryptionDiagnostics.report()).contains("SEND_REQUEST")
        assertThat(SyncMeDiagnosticsControl.decryptionEnabled(context)).isTrue()
    }

    @Test
    fun `identifiers are hashed and disabled capture preserves existing report`() {
        SyncMeDiagnosticsControl.setDecryptionEnabled(context, true)
        DecryptionDiagnostics.record(DecryptionDiagnostics.Stage.EVENT_SESSION, "private-event-id", relatedIdentifier = "private-session-id")
        val before = DecryptionDiagnostics.report()
        assertThat(before).contains("EVENT_SESSION")
        assertThat(before).doesNotContain("private-event-id")
        assertThat(before).doesNotContain("private-session-id")
        SyncMeDiagnosticsControl.setDecryptionEnabled(context, false)
        DecryptionDiagnostics.record(DecryptionDiagnostics.Stage.SEND_REJECTED)
        assertThat(DecryptionDiagnostics.report()).doesNotContain("SEND_REJECTED")
        assertThat(DecryptionDiagnostics.report()).contains("EVENT_SESSION")
        DecryptionDiagnostics.clear()
        assertThat(DecryptionDiagnostics.report()).doesNotContain("EVENT_SESSION")
    }
}
