/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.home.impl.updates

import android.content.pm.PackageInfo
import android.content.pm.Signature
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [26])
@Suppress("DEPRECATION")
class UpdateReleaseTest : RobolectricTest() {
    private fun metadata() = JSONObject().put("version", "1.1.12").put("versionCode", 202611042)
        .put("size", 73746021).put("sha256", "a".repeat(64)).put("url", UPDATE_APK)

    @Test
    fun `valid metadata preserves the actual package version and hash`() {
        val release = UpdateRelease.parse(metadata().toString())
        assertThat(release.version).isEqualTo("1.1.12")
        assertThat(release.versionCode).isEqualTo(202611042)
        assertThat(release.size).isEqualTo(73746021)
        assertThat(release.sha256).isEqualTo("a".repeat(64))
    }

    @Test
    fun `download URL cannot switch host path or protocol`() {
        listOf("http://example.org/SyncMe.apk", "https://example.org/SyncMe.apk", "$UPDATE_APK?alternate=true").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { UpdateRelease.parse(metadata().put("url", url).toString()) }
        }
    }

    @Test
    fun `malformed hash zero size oversized file and invalid version are rejected`() {
        listOf("sha256" to "bad", "size" to 0, "size" to 536870913L, "versionCode" to -1, "version" to "<b>new</b>").forEach { (key, value) ->
            assertThrows(IllegalArgumentException::class.java) { UpdateRelease.parse(metadata().put(key, value).toString()) }
        }
        assertThrows(IllegalArgumentException::class.java) { UpdateRelease.parse(" ".repeat(65537)) }
    }

    private fun info(code: Int, name: String = "ir.syncme.app.syncmerelease", signer: String = "001122") = PackageInfo().apply {
        packageName = name
        versionCode = code
        signatures = arrayOf(Signature(signer))
    }

    @Test
    fun `newer package signed by the installed signer is accepted`() {
        requireCompatibleUpdate(info(2), info(1), 2)
    }

    @Test
    fun `different package signer wrong version and downgrade are rejected`() {
        listOf(info(2, name = "other.app"), info(2, signer = "112233"), info(3), info(1)).forEach { archive ->
            assertThrows(IllegalArgumentException::class.java) { requireCompatibleUpdate(archive, info(1), 2) }
        }
        assertThrows(IllegalArgumentException::class.java) { requireCompatibleUpdate(info(1), info(2), 1) }
        assertThrows(IllegalArgumentException::class.java) { requireCompatibleUpdate(info(2).apply { signatures = emptyArray() }, info(1), 2) }
    }
}
