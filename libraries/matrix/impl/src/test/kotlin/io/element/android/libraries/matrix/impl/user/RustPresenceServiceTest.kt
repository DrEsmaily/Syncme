package io.element.android.libraries.matrix.impl.user

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.sync.SyncState
import io.element.android.libraries.matrix.api.user.UserPresence
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RustPresenceServiceTest : RobolectricTest() {
    private val user = UserId("@bob:example.org")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sync = MutableStateFlow(SyncState.Running)
    private val visibility = MutableStateFlow<Boolean?>(true)
    private val failSync = AtomicBoolean(false)
    private val slowSnapshots = AtomicBoolean(false)
    private lateinit var server: MockWebServer
    private lateinit var service: RustPresenceService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.requestUrl!!.encodedPath.endsWith("/sync")) {
                    if (failSync.get()) return MockResponse().setResponseCode(503)
                    return MockResponse().setBody("""{"next_batch":"next","presence":{"events":[
                        {"type":"m.presence","sender":"@bob:example.org","content":{"presence":"online"}}
                    ]}}""")
                }
                return MockResponse().setBody("""{"presence":"unavailable"}""").apply {
                    if (slowSnapshots.get()) setBodyDelay(2, TimeUnit.SECONDS)
                }
            }
        }
        server.start()
        service = RustPresenceService(
            server.url("/").toString(),
            UserId("@alice:example.org"),
            InMemorySessionStore(listOf(aSessionData())),
            sync,
            visibility,
            scope,
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    @Test
    fun `two screens share registration and release only after both leave`() = runBlocking {
        service.track(setOf(user))
        service.track(setOf(user))
        eventually { service.states.value[user] == UserPresence.Online }
        service.release(setOf(user))
        assertThat(service.states.value[user]).isEqualTo(UserPresence.Online)
        service.release(setOf(user))
        assertThat(service.states.value).isEmpty()
        delay(700)
        assertThat(service.states.value).isEmpty()
    }

    @Test
    fun `sync stop immediately removes online and late snapshot cannot restore it`() = runBlocking {
        slowSnapshots.set(true)
        service.track(setOf(user))
        eventually { service.states.value[user] == UserPresence.Online }
        sync.value = SyncState.Offline
        eventually { service.states.value.isEmpty() }
        delay(2200)
        assertThat(service.states.value).isEmpty()
        sync.value = SyncState.Running
        eventually { service.states.value[user] == UserPresence.Online }
    }

    @Test
    fun `background requests away and foreground restores online without forced offline`() = runBlocking {
        service.track(setOf(user))
        eventually { service.states.value[user] == UserPresence.Online }
        visibility.value = false
        var awayRequest: RecordedRequest? = null
        withTimeout(5000) {
            while (awayRequest == null) {
                val request = server.takeRequest(100, TimeUnit.MILLISECONDS)
                if (request?.requestUrl?.queryParameter("set_presence") == "unavailable") awayRequest = request
            }
        }
        assertThat(awayRequest!!.requestUrl!!.queryParameter("set_presence")).isEqualTo("unavailable")
        visibility.value = true
        withTimeout(5000) {
            var found = false
            while (!found) {
                val request = server.takeRequest(100, TimeUnit.MILLISECONDS)
                found = request?.requestUrl?.queryParameter("set_presence") == "online"
            }
        }
    }

    @Test
    fun `server failure clears presence and reconnect refreshes full state`() = runBlocking {
        service.track(setOf(user))
        eventually { service.states.value[user] == UserPresence.Online }
        failSync.set(true)
        eventually { service.states.value.isEmpty() }
        failSync.set(false)
        eventually { service.states.value[user] == UserPresence.Online }
    }

    private suspend fun eventually(condition: () -> Boolean) = withTimeout(6000) {
        while (!condition()) delay(10)
    }
}
