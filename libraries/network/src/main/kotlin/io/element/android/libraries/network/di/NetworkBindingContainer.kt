/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.network.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.network.interceptors.DynamicHttpLoggingInterceptor
import io.element.android.libraries.network.interceptors.FormattedJsonHttpLogger
import io.element.android.libraries.network.interceptors.UserAgentInterceptor
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@BindingContainer
@ContributesTo(AppScope::class)
object NetworkBindingContainer {
    /**
     * SyncMe 234 TLS connectivity experiment. HTTPS is still used, but certificate
     * chains and hostnames are NOT authenticated. Do not distribute as secure.
     */
    private val permissiveTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesOkHttpClient(
        userAgentInterceptor: UserAgentInterceptor,
        dynamicHttpLoggingInterceptor: DynamicHttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(60, TimeUnit.SECONDS)
        writeTimeout(60, TimeUnit.SECONDS)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(permissiveTrustManager), SecureRandom())
        }
        sslSocketFactory(sslContext.socketFactory, permissiveTrustManager)
        hostnameVerifier { _, _ -> true }
        addInterceptor(userAgentInterceptor)
        addInterceptor(dynamicHttpLoggingInterceptor)
    }.build()

    @Provides
    fun providesCallFactory(okHttpClient: OkHttpClient): Call.Factory = Call.Factory { request -> okHttpClient.newCall(request) }

    @Provides
    @SingleIn(AppScope::class)
    fun providesHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = FormattedJsonHttpLogger(HttpLoggingInterceptor.Level.BODY)
        return HttpLoggingInterceptor(logger)
    }
}
