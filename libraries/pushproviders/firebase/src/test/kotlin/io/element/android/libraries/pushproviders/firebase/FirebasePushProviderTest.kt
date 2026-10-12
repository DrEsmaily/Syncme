/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.pushproviders.firebase

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.push.test.FakePusherSubscriber
import io.element.android.libraries.pushproviders.api.Config
import io.element.android.libraries.pushproviders.api.Distributor
import io.element.android.libraries.pushproviders.api.PusherSubscriber
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.lambda.value
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FirebasePushProviderTest {
    @Test
    fun `test index and name`() {
        val firebasePushProvider = createFirebasePushProvider()
        assertThat(firebasePushProvider.name).isEqualTo(FirebaseConfig.NAME)
        assertThat(firebasePushProvider.index).isEqualTo(FirebaseConfig.INDEX)
    }

    @Test
    fun `getDistributors return the unique distributor if available`() {
        val firebasePushProvider = createFirebasePushProvider(
            isPlayServiceAvailable = FakeIsPlayServiceAvailable(isAvailable = true)
        )
        val result = firebasePushProvider.getDistributors()
        assertThat(result).containsExactly(Distributor("Firebase", "Firebase"))
    }

    @Test
    fun `getDistributors return empty list if service is not available`() {
        val firebasePushProvider = createFirebasePushProvider(
            isPlayServiceAvailable = FakeIsPlayServiceAvailable(isAvailable = false)
        )
        val result = firebasePushProvider.getDistributors()
        assertThat(result).isEmpty()
    }

    @Test
    fun `getCurrentDistributor always returns the unique distributor`() = runTest {
        val firebasePushProvider = createFirebasePushProvider()
        val result = firebasePushProvider.getCurrentDistributor(A_SESSION_ID)
        assertThat(result).isEqualTo(Distributor("Firebase", "Firebase"))
    }

    @Test
    fun `register ok`() = runTest {
        val matrixClient = FakeMatrixClient()
        val registerPusherResultLambda = lambdaRecorder<MatrixClient, String, String, Result<Unit>> { _, _, _ -> Result.success(Unit) }
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = "aToken"
            ),
            pusherSubscriber = FakePusherSubscriber(
                registerPusherResult = registerPusherResultLambda
            )
        )
        val result = firebasePushProvider.registerWith(matrixClient, Distributor("value", "Name"))
        assertThat(result).isEqualTo(Result.success(Unit))
        registerPusherResultLambda.assertions()
            .isCalledOnce()
            .with(value(matrixClient), value("aToken"), value(A_FIREBASE_GATEWAY))
    }

    @Test
    fun `register ko no token`() = runTest {
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = null
            ),
            pusherSubscriber = FakePusherSubscriber(
                registerPusherResult = { _, _, _ -> Result.success(Unit) }
            )
        )
        val result = firebasePushProvider.registerWith(FakeMatrixClient(), Distributor("value", "Name"))
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `register ko error`() = runTest {
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = "aToken"
            ),
            pusherSubscriber = FakePusherSubscriber(
                registerPusherResult = { _, _, _ -> Result.failure(AN_EXCEPTION) }
            )
        )
        val result = firebasePushProvider.registerWith(FakeMatrixClient(), Distributor("value", "Name"))
        assertThat(result.isFailure).isTrue()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `registration waits for a late nonblank token`() = runTest {
        val store = InMemoryFirebaseStore("")
        var registeredKey: String? = null
        val provider = createFirebasePushProvider(
            firebaseStore = store,
            pusherSubscriber = FakePusherSubscriber(registerPusherResult = { _, key, _ ->
                registeredKey = key
                Result.success(Unit)
            }),
        )
        val registration = async { provider.registerWith(FakeMatrixClient(), Distributor("Firebase", "Firebase")) }
        runCurrent()
        assertThat(registration.isCompleted).isFalse()
        store.storeInstallationId(" ")
        runCurrent()
        assertThat(registeredKey).isNull()
        advanceTimeBy(19_000)
        store.storeInstallationId("late-token")
        assertThat(registration.await().isSuccess).isTrue()
        assertThat(registeredKey).isEqualTo("late-token")
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `missing token times out without registering an empty pusher`() = runTest {
        var called = false
        val provider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(),
            pusherSubscriber = FakePusherSubscriber(registerPusherResult = { _, _, _ ->
                called = true
                Result.success(Unit)
            }),
        )
        val registration = async { provider.registerWith(FakeMatrixClient(), Distributor("Firebase", "Firebase")) }
        runCurrent()
        advanceTimeBy(19_999)
        assertThat(registration.isCompleted).isFalse()
        advanceTimeBy(1)
        assertThat(registration.await().isFailure).isTrue()
        assertThat(called).isFalse()
    }

    @Test
    fun `completed token flow returns a failure instead of throwing`() = runTest {
        val store = object : FirebaseStore {
            override fun getInstallationId(): String? = null
            override fun fcmInstallationIdFlow() = flowOf<String?>(null, "")
            override fun storeInstallationId(installationId: String?) = Unit
        }
        val result = createFirebasePushProvider(firebaseStore = store)
            .registerWith(FakeMatrixClient(), Distributor("Firebase", "Firebase"))
        assertThat(result.isFailure).isTrue()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelled registration does not register when a token arrives later`() = runTest {
        val store = InMemoryFirebaseStore()
        var called = false
        val provider = createFirebasePushProvider(
            firebaseStore = store,
            pusherSubscriber = FakePusherSubscriber(registerPusherResult = { _, _, _ ->
                called = true
                Result.success(Unit)
            }),
        )
        val registration = async { provider.registerWith(FakeMatrixClient(), Distributor("Firebase", "Firebase")) }
        runCurrent()
        registration.cancel()
        registration.join()
        store.storeInstallationId("late-token")
        runCurrent()
        assertThat(registration.isCancelled).isTrue()
        assertThat(called).isFalse()
    }

    @Test
    fun `unregister ok`() = runTest {
        val matrixClient = FakeMatrixClient()
        val unregisterPusherResultLambda = lambdaRecorder<MatrixClient, String, String, Result<Unit>> { _, _, _ -> Result.success(Unit) }
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = "aToken"
            ),
            pusherSubscriber = FakePusherSubscriber(
                unregisterPusherResult = unregisterPusherResultLambda
            )
        )
        val result = firebasePushProvider.unregister(matrixClient)
        assertThat(result).isEqualTo(Result.success(Unit))
        unregisterPusherResultLambda.assertions()
            .isCalledOnce()
            .with(value(matrixClient), value("aToken"), value(A_FIREBASE_GATEWAY))
    }

    @Test
    fun `unregister no token - in this case, the error is ignored`() = runTest {
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = null
            ),
        )
        val result = firebasePushProvider.unregister(FakeMatrixClient())
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `unregister ko error`() = runTest {
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = "aToken"
            ),
            pusherSubscriber = FakePusherSubscriber(
                unregisterPusherResult = { _, _, _ -> Result.failure(AN_EXCEPTION) }
            )
        )
        val result = firebasePushProvider.unregister(FakeMatrixClient())
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getCurrentUserPushConfig no push ket`() = runTest {
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = null
            )
        )
        val result = firebasePushProvider.getPushConfig(A_SESSION_ID)
        assertThat(result).isNull()
    }

    @Test
    fun `getCurrentUserPushConfig ok`() = runTest {
        val firebasePushProvider = createFirebasePushProvider(
            firebaseStore = InMemoryFirebaseStore(
                installationId = "aToken"
            ),
        )
        val result = firebasePushProvider.getPushConfig(A_SESSION_ID)
        assertThat(result).isEqualTo(Config(A_FIREBASE_GATEWAY, "aToken"))
    }

    @Test
    fun `rotateToken invokes the FirebaseMessagingSessionRotator`() = runTest {
        val lambda = lambdaRecorder<Result<Unit>> { Result.success(Unit) }
        val firebasePushProvider = createFirebasePushProvider(
            rotateFirebaseSession = FakeRotateFirebaseSession(lambda),
        )
        firebasePushProvider.rotateToken()
        lambda.assertions().isCalledOnce()
    }

    @Test
    fun `canRotateToken should return true`() = runTest {
        val firebasePushProvider = createFirebasePushProvider()
        assertThat(firebasePushProvider.canRotateToken()).isTrue()
    }

    @Test
    fun `onSessionDeleted should be noop`() = runTest {
        val firebasePushProvider = createFirebasePushProvider()
        firebasePushProvider.onSessionDeleted(A_SESSION_ID)
    }

    private fun createFirebasePushProvider(
        firebaseStore: FirebaseStore = InMemoryFirebaseStore(),
        pusherSubscriber: PusherSubscriber = FakePusherSubscriber(),
        isPlayServiceAvailable: IsPlayServiceAvailable = FakeIsPlayServiceAvailable(false),
        rotateFirebaseSession: RotateFirebaseSession = FakeRotateFirebaseSession(),
        firebaseGatewayProvider: FirebaseGatewayProvider = FakeFirebaseGatewayProvider()
    ): FirebasePushProvider {
        return FirebasePushProvider(
            firebaseStore = firebaseStore,
            pusherSubscriber = pusherSubscriber,
            isPlayServiceAvailable = isPlayServiceAvailable,
            rotateFirebaseSession = rotateFirebaseSession,
            firebaseGatewayProvider = firebaseGatewayProvider,
        )
    }
}
