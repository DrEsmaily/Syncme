/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.preferences.impl.store

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.launch
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.core.meta.BuildType
import io.element.android.libraries.preferences.test.FakePreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultAppPreferencesStoreTest {
    private val buildMeta = BuildMeta(
        buildType = BuildType.DEBUG,
        isDebuggable = true,
        applicationName = "Element X",
        productionApplicationName = "Element",
        desktopApplicationName = "Element Desktop",
        applicationId = "io.element.android",
        isEnterpriseBuild = false,
        lowPrivacyLoggingEnabled = false,
        versionName = "1.0.0",
        versionCode = 1,
        gitRevision = "test",
        gitBranchName = "test",
        flavorDescription = "test",
        flavorShortDescription = "test",
    )

    @Test
    fun `custom tag selections migrate persist and toggle independently`() = runTest {
        val backing = FakePreferenceDataStoreFactory().create("legacy-tags")
        val factory = object : io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory {
            override fun create(name: String) = backing
        }
        val store = DefaultAppPreferencesStore(buildMeta, factory)
        val first = requireNotNull(store.createCustomRoomTag("Family", "TAG"))
        val second = requireNotNull(store.createCustomRoomTag("Friends", "TAG"))
        backing.edit { it[stringPreferencesKey("syncmeActiveCustomRoomTagId")] = first }
        assertThat(store.getActiveCustomRoomTagIdsFlow().first()).containsExactly(first)
        store.toggleActiveCustomRoomTag(second)
        val restored = DefaultAppPreferencesStore(buildMeta, factory)
        assertThat(restored.getActiveCustomRoomTagIdsFlow().first()).containsExactly(first, second)
        restored.toggleActiveCustomRoomTag(first)
        assertThat(restored.getActiveCustomRoomTagIdsFlow().first()).containsExactly(second)
        restored.toggleActiveCustomRoomTag("missing-tag")
        assertThat(restored.getActiveCustomRoomTagIdsFlow().first()).containsExactly(second)
        restored.deleteCustomRoomTag(second)
        assertThat(restored.getActiveCustomRoomTagIdsFlow().first()).isEmpty()
        restored.toggleActiveCustomRoomTag(first)
        restored.clearActiveCustomRoomTags()
        assertThat(restored.getActiveCustomRoomTagIdsFlow().first()).isEmpty()
    }

    @Test
    fun `rapid custom tag selections do not overwrite each other`() = runTest {
        val store = createStore()
        val first = requireNotNull(store.createCustomRoomTag("Family", "TAG"))
        val second = requireNotNull(store.createCustomRoomTag("Friends", "TAG"))
        kotlinx.coroutines.coroutineScope {
            launch { store.toggleActiveCustomRoomTag(first) }
            launch { store.toggleActiveCustomRoomTag(second) }
        }
        assertThat(store.getActiveCustomRoomTagIdsFlow().first()).containsExactly(first, second)
    }

    @Test
    fun `live location minimum distance defaults to 10`() = runTest {
        val store = DefaultAppPreferencesStore(
            buildMeta = buildMeta,
            preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
        )

        assertThat(store.getLiveLocationMinimumDistanceInMetersUpdateFlow().first()).isEqualTo(10)
    }

    @Test
    fun `live location minimum distance persists updates`() = runTest {
        val store = DefaultAppPreferencesStore(
            buildMeta = buildMeta,
            preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
        )

        store.setLiveLocationMinimumDistanceInMetersUpdate(25)

        assertThat(store.getLiveLocationMinimumDistanceInMetersUpdateFlow().first()).isEqualTo(25)
    }

    @Test
    fun `homeserver history is empty by default`() = runTest {
        val store = createStore()
        assertThat(store.getHomeserverHistoryFlow().first()).isEmpty()
    }

    @Test
    fun `adding homeservers keeps the most recent first`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("https://matrix.org")
        store.addHomeserverToHistory("https://example.com")

        assertThat(store.getHomeserverHistoryFlow().first())
            .containsExactly("https://example.com", "https://matrix.org")
            .inOrder()
    }

    @Test
    fun `adding an existing homeserver moves it to the front without duplicating`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("https://matrix.org")
        store.addHomeserverToHistory("https://example.com")
        store.addHomeserverToHistory("https://matrix.org")

        assertThat(store.getHomeserverHistoryFlow().first())
            .containsExactly("https://matrix.org", "https://example.com")
            .inOrder()
    }

    @Test
    fun `homeservers are normalised and deduplicated case-insensitively`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("  HTTPS://Matrix.ORG  ")
        store.addHomeserverToHistory("https://matrix.org")

        assertThat(store.getHomeserverHistoryFlow().first())
            .containsExactly("https://matrix.org")
    }

    @Test
    fun `blank homeservers are ignored`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("   ")

        assertThat(store.getHomeserverHistoryFlow().first()).isEmpty()
    }

    @Test
    fun `homeserver history is capped to the most recent entries`() = runTest {
        val store = createStore()

        repeat(25) { index ->
            store.addHomeserverToHistory("https://server$index.org")
        }

        val history = store.getHomeserverHistoryFlow().first()
        assertThat(history).hasSize(20)
        assertThat(history.first()).isEqualTo("https://server24.org")
        assertThat(history.last()).isEqualTo("https://server5.org")
    }

    @Test
    fun `other accounts section is expanded by default`() = runTest {
        val store = createStore()

        assertThat(store.isOtherAccountsExpandedFlow().first()).isTrue()
    }

    @Test
    fun `other accounts expanded state persists updates`() = runTest {
        val store = createStore()

        store.setOtherAccountsExpanded(false)
        assertThat(store.isOtherAccountsExpandedFlow().first()).isFalse()

        store.setOtherAccountsExpanded(true)
        assertThat(store.isOtherAccountsExpandedFlow().first()).isTrue()
    }

    private fun createStore() = DefaultAppPreferencesStore(
        buildMeta = buildMeta,
        preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
    )
}
