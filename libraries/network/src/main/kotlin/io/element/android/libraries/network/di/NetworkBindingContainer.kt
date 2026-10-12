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
import io.element.android.libraries.matrix.api.diagnostics.TlsAudit
import io.element.android.libraries.network.interceptors.DynamicHttpLoggingInterceptor
import io.element.android.libraries.network.interceptors.FormattedJsonHttpLogger
import io.element.android.libraries.network.interceptors.UserAgentInterceptor
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.Protocol
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@BindingContainer
@ContributesTo(AppScope::class)
object NetworkBindingContainer {
    @Provides
    @SingleIn(AppScope::class)
    fun providesOkHttpClient(
        userAgentInterceptor: UserAgentInterceptor,
        dynamicHttpLoggingInterceptor: DynamicHttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(60, TimeUnit.SECONDS)
        writeTimeout(60, TimeUnit.SECONDS)
        eventListenerFactory { _ ->
            object : EventListener() {
                override fun callStart(call: Call) {
                    val url = call.request().url
                    TlsAudit.network("OKHTTP_CALL_START", url.host, "scheme=" + url.scheme +
                        " port=" + url.port + " method=" + call.request().method)
                }

                override fun dnsStart(call: Call, domainName: String) {
                    TlsAudit.network("OKHTTP_DNS_START", domainName, caller = true)
                }

                override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
                    val ips = inetAddressList.take(8).joinToString(",") { it.hostAddress.orEmpty() }
                    TlsAudit.network("OKHTTP_DNS_END", domainName, "ip=" + ips)
                }

                override fun secureConnectStart(call: Call) {
                    TlsAudit.network("OKHTTP_TLS_HANDSHAKE_START", call.request().url.host)
                }

                override fun secureConnectEnd(call: Call, handshake: Handshake?) {
                    TlsAudit.network("OKHTTP_TLS_HANDSHAKE_END", call.request().url.host,
                        "tls=" + (handshake?.tlsVersion ?: "unknown") +
                        " cipher=" + (handshake?.cipherSuite ?: "unknown"))
                }

                override fun connectFailed(
                    call: Call,
                    inetSocketAddress: InetSocketAddress,
                    proxy: Proxy,
                    protocol: Protocol?,
                    ioe: IOException,
                ) {
                    TlsAudit.network("OKHTTP_CONNECT_FAILED", call.request().url.host,
                        "remote=" + inetSocketAddress.address?.hostAddress +
                        " port=" + inetSocketAddress.port +
                        " error=" + ioe.javaClass.simpleName, caller = true)
                }

                override fun callFailed(call: Call, ioe: IOException) {
                    TlsAudit.network("OKHTTP_CALL_FAILED", call.request().url.host,
                        "error=" + ioe.javaClass.simpleName, caller = true)
                }
            }
        }
        addInterceptor(userAgentInterceptor)
        addInterceptor(dynamicHttpLoggingInterceptor)
    }.build()

    @Provides
    fun providesCallFactory(okHttpClient: OkHttpClient): Call.Factory = Call.Factory { request ->
        TlsAudit.network("OKHTTP_CALL_ORIGIN", request.url.host,
            "method=" + request.method + " port=" + request.url.port, caller = true)
        okHttpClient.newCall(request)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = FormattedJsonHttpLogger(HttpLoggingInterceptor.Level.BODY)
        return HttpLoggingInterceptor(logger)
    }
}
