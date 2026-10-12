package io.element.android.libraries.matrix.impl.user

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.user.UserPresence
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.json.JSONObject
import org.junit.Test

class PresenceFromJsonTest : RobolectricTest() {
    @Test
    fun `only explicit online and unavailable map to online and away`() {
        assertThat(presenceFromJson(JSONObject("""{"presence":"online","currently_active":false}"""), 1000)).isEqualTo(UserPresence.Online)
        assertThat(presenceFromJson(JSONObject("""{"presence":"unavailable"}"""), 1000)).isEqualTo(UserPresence.Away)
        assertThat(presenceFromJson(JSONObject(), 1000)).isEqualTo(UserPresence.Unknown)
        assertThat(presenceFromJson(JSONObject("""{"presence":"busy"}"""), 1000)).isEqualTo(UserPresence.Unknown)
    }

    @Test
    fun `offline last seen rejects missing negative and overflowing ages`() {
        for (age in listOf(-1L, 2000L, Long.MAX_VALUE)) {
            assertThat(presenceFromJson(JSONObject().put("presence", "offline").put("last_active_ago", age), 1000))
                .isEqualTo(UserPresence.Offline(null))
        }
        assertThat(presenceFromJson(JSONObject("""{"presence":"offline","last_active_ago":100}"""), 1000))
            .isEqualTo(UserPresence.Offline(900))
    }
}
