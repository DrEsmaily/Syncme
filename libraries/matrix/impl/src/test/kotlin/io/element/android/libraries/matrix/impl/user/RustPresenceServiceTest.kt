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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class RustPresenceServiceTest : RobolectricTest() {
    private val user = UserId("@bob:example.org")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sync = MutableStateFlow(SyncState.Running)
    private val visibility = MutableStateFlow<Boolean?>(true)
    private val failRequests = AtomicBoolean(false)
    private val presence = AtomicReference("online")
    private val published = CopyOnWriteArrayList<String>()
    private val paths = CopyOnWriteArrayList<String>()
    private val slowSnapshots = AtomicBoolean(false)
    private lateinit var server: MockWebServer
    private lateinit var service: RustPresenceService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                paths.add(request.requestUrl!!.encodedPath)
                if (failRequests.get()) return MockResponse().setResponseCode(503)
                return MockResponse().setBody("""{"presence":"${presence.get()}"}""").apply {
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
        eventually { "unavailable" in published }
        eventually { service.states.value.isEmpty() }
        visibility.value = true
        eventually { published.count { it == "online" } == 2 }
        eventually { service.states.value[user] == UserPresence.Online }
        assertThat(published).doesNotContain("offline")
    }

    @Test
    fun `server failure clears presence and later successful snapshots restore it`() = runBlocking {
        service.track(setOf(user))
        eventually { service.states.value[user] == UserPresence.Online }
        failRequests.set(true)
        eventually { service.states.value.isEmpty() }
        failRequests.set(false)
        eventually { service.states.value[user] == UserPresence.Online }
    }

    @Test
    fun `online changes to away from a fresh server response`() = runBlocking {
        service.track(setOf(user))
        eventually { service.states.value[user] == UserPresence.Online }
        presence.set("unavailable")
        eventually { service.states.value[user] == UserPresence.Away }
    }

    @Test
    fun `presence never calls sync or acknowledges to device encryption messages`() = runBlocking {
        service.track(setOf(user))
        eventually { service.states.value[user] == UserPresence.Online }
        delay(500)
        assertThat(paths).isNotEmpty()
        assertThat(paths.all { it.startsWith("/_matrix/client/v3/presence/") && it.endsWith("/status") }).isTrue()
    }

    private suspend fun eventually(condition: () -> Boolean) = withTimeout(6000) {
        while (!condition()) delay(10)
    }
}
