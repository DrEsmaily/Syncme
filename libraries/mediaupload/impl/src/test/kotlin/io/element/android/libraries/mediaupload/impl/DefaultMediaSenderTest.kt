/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaupload.impl

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.media.FileInfo
import io.element.android.libraries.matrix.api.media.ImageInfo
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.test.A_THREAD_ID
import io.element.android.libraries.matrix.test.media.FakeMediaUploadHandler
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.matrix.test.timeline.FakeTimeline
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfig
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfigProvider
import io.element.android.libraries.mediaupload.api.MediaPreProcessor
import io.element.android.libraries.mediaupload.test.FakeMediaPreProcessor
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class DefaultMediaSenderTest : RobolectricTest() {
    private val mediaOptimizationConfig = MediaOptimizationConfig(
        compressImages = true,
        videoCompressionPreset = VideoCompressionPreset.STANDARD,
    )

    @Test
    fun `given an attachment when sending it the preprocessor always runs`() = runTest {
        val preProcessor = FakeMediaPreProcessor()
        val sender = createDefaultMediaSender(
            preProcessor = preProcessor,
            room = FakeJoinedRoom(
                liveTimeline = FakeTimeline().apply {
                    sendFileLambda = lambdaRecorder<
                        File,
                        FileInfo,
                        String?,
                        String?,
                        EventId?,
                        Result<FakeMediaUploadHandler>,
                        > { _, _, _, _, _ ->
                        Result.success(FakeMediaUploadHandler())
                    }
                },
            )
        )

        val uri = Uri.parse("content://image.jpg")
        sender.sendMedia(uri = uri, mimeType = MimeTypes.Jpeg, mediaOptimizationConfig = mediaOptimizationConfig)

        assertThat(preProcessor.processCallCount).isEqualTo(1)
    }

    @Test
    fun `given an attachment when sending it the Room will call sendMedia`() = runTest {
        val sendImageResult =
            lambdaRecorder { _: File, _: File?, _: ImageInfo, _: String?, _: String?, _: EventId? ->
                Result.success(FakeMediaUploadHandler())
            }
        val room = FakeJoinedRoom(
            liveTimeline = FakeTimeline().apply {
                sendImageLambda = sendImageResult
            },
        )
        val sender = createDefaultMediaSender(room = room)

        val uri = Uri.parse("content://image.jpg")
        sender.sendMedia(uri = uri, mimeType = MimeTypes.Jpeg, mediaOptimizationConfig = mediaOptimizationConfig)
    }

    @Test
    fun `given a failure in the preprocessor when sending the whole process fails`() = runTest {
        val preProcessor = FakeMediaPreProcessor().apply {
            givenResult(Result.failure(Exception()))
        }
        val sender = createDefaultMediaSender(preProcessor)

        val uri = Uri.parse("content://image.jpg")
        val result = sender.sendMedia(uri = uri, mimeType = MimeTypes.Jpeg, mediaOptimizationConfig = mediaOptimizationConfig)

        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun `given a failure in the media upload when sending the whole process fails`() = runTest {
        val preProcessor = FakeMediaPreProcessor().apply {
            givenImageResult()
        }
        val sendImageResult =
            lambdaRecorder { _: File, _: File?, _: ImageInfo, _: String?, _: String?, _: EventId? ->
                Result.failure<FakeMediaUploadHandler>(Exception())
            }
        val room = FakeJoinedRoom(
            liveTimeline = FakeTimeline().apply {
                sendImageLambda = sendImageResult
            },
        )
        val sender = createDefaultMediaSender(
            preProcessor = preProcessor,
            room = room,
        )

        val uri = Uri.parse("content://image.jpg")
        val result = sender.sendMedia(uri = uri, mimeType = MimeTypes.Jpeg, mediaOptimizationConfig = mediaOptimizationConfig)

        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a cancellation in the media upload when sending the job is cancelled`() = runTest(StandardTestDispatcher()) {
        val sendFileResult =
            lambdaRecorder<File, FileInfo, String?, String?, EventId?, Result<FakeMediaUploadHandler>> { _, _, _, _, _ ->
                Result.success(FakeMediaUploadHandler())
            }
        val room = FakeJoinedRoom(
            liveTimeline = FakeTimeline().apply {
                sendFileLambda = sendFileResult
            },
        )
        val sender = createDefaultMediaSender(room = room)
        val sendJob = launch {
            val uri = Uri.parse("content://image.jpg")
            sender.sendMedia(uri = uri, mimeType = MimeTypes.Jpeg, mediaOptimizationConfig = mediaOptimizationConfig)
        }
        // Wait until several internal tasks run and the file is being uploaded
        advanceTimeBy(3L)

        // Assert the file is being uploaded
        assertThat(sender.hasOngoingMediaUploads).isTrue()

        // Cancel the coroutine
        sendJob.cancel()

        // Wait for the coroutine cleanup to happen
        advanceTimeBy(1L)

        // Assert the file is not being uploaded anymore
        assertThat(sender.hasOngoingMediaUploads).isFalse()
        sendFileResult.assertions().isCalledOnce()
    }

    @Test
    fun `given a thread, the created timeline is closed once the media is sent`() = runTest {
        val threadedTimeline = FakeTimeline(mode = Timeline.Mode.Thread(A_THREAD_ID)).apply {
            sendFileLambda = { _, _, _, _, _ -> Result.success(FakeMediaUploadHandler()) }
        }
        val liveTimeline = FakeTimeline()
        val room = FakeJoinedRoom(
            liveTimeline = liveTimeline,
            createTimelineResult = { Result.success(threadedTimeline) },
        )
        val sender = createDefaultMediaSender(
            room = room,
            timelineMode = Timeline.Mode.Thread(A_THREAD_ID),
        )

        val uri = Uri.parse("content://image.jpg")
        val result = sender.sendMedia(uri = uri, mimeType = MimeTypes.Jpeg, mediaOptimizationConfig = mediaOptimizationConfig)

        assertThat(result.isSuccess).isTrue()
        assertThat(threadedTimeline.closeCounter).isEqualTo(1)
        assertThat(liveTimeline.closeCounter).isEqualTo(0)
    }

    @Test
    fun `given a live timeline, the timeline is not closed once the media is sent`() = runTest {
        val liveTimeline = FakeTimeline().apply {
            sendFileLambda = { _, _, _, _, _ -> Result.success(FakeMediaUploadHandler()) }
        }
        val sender = createDefaultMediaSender(room = FakeJoinedRoom(liveTimeline = liveTimeline))

        val uri = Uri.parse("content://image.jpg")
        val result = sender.sendMedia(uri = uri, mimeType = MimeTypes.Jpeg, mediaOptimizationConfig = mediaOptimizationConfig)

        assertThat(result.isSuccess).isTrue()
        assertThat(liveTimeline.closeCounter).isEqualTo(0)
    }

    @Test
    fun `album preserves order caption reply and awaits the thread upload before closing`() = runTest {
        val first = io.element.android.libraries.mediaupload.api.MediaUploadInfo.Image(File("first"), ImageInfo(width = 10L, height = 10L, mimetype = MimeTypes.Jpeg, size = 10L, thumbnailInfo = null, thumbnailSource = null, blurhash = null), null)
        val second = first.copy(file = File("second"))
        var uploaded = false
        val thread = FakeTimeline(mode = Timeline.Mode.Thread(A_THREAD_ID)).apply {
            val activeTimeline = this
            sendGalleryLambda = { items, caption, formatted, reply ->
                assertThat(items.map { (it as io.element.android.libraries.matrix.api.media.GalleryItemInfo.Image).file.name }).containsExactly("first", "second").inOrder()
                assertThat(caption).isEqualTo("Album caption")
                assertThat(formatted).isNull()
                assertThat(reply).isEqualTo(EventId("\$album-reply"))
                Result.success(object : io.element.android.libraries.matrix.api.media.MediaUploadHandler {
                    override suspend fun await(): Result<Unit> {
                        assertThat(activeTimeline.closeCounter).isEqualTo(0)
                        uploaded = true
                        return Result.success(Unit)
                    }
                    override fun cancel() = Unit
                })
            }
        }
        val sender = createDefaultMediaSender(room = FakeJoinedRoom(createTimelineResult = { Result.success(thread) }), timelineMode = Timeline.Mode.Thread(A_THREAD_ID))
        val result = sender.sendGallery(listOf(first, second), "Album caption", null, EventId("\$album-reply"))
        assertThat(result.isSuccess).isTrue()
        assertThat(uploaded).isTrue()
        assertThat(thread.closeCounter).isEqualTo(1)
    }

    @Test
    fun `album upload failure reaches caller without reporting success`() = runTest {
        val error = java.io.IOException("Offline")
        val timeline = FakeTimeline().apply { sendGalleryLambda = { _, _, _, _ -> Result.success(FakeMediaUploadHandler(Result.failure(error))) } }
        val sender = createDefaultMediaSender(room = FakeJoinedRoom(liveTimeline = timeline))
        val info = io.element.android.libraries.mediaupload.api.MediaUploadInfo.AnyFile(File("image"), FileInfo(mimetype = MimeTypes.Jpeg, size = 10, thumbnailInfo = null, thumbnailSource = null))
        val result = sender.sendGallery(listOf(info, info.copy(file = File("second"))), null, null, null)
        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
        assertThat(timeline.closeCounter).isEqualTo(0)
    }

    private fun createDefaultMediaSender(
        preProcessor: MediaPreProcessor = FakeMediaPreProcessor(),
        room: JoinedRoom = FakeJoinedRoom(),
        timelineMode: Timeline.Mode = Timeline.Mode.Live,
        mediaOptimizationConfigProvider: MediaOptimizationConfigProvider = MediaOptimizationConfigProvider { mediaOptimizationConfig },
    ) = DefaultMediaSender(
        preProcessor = preProcessor,
        room = room,
        timelineMode = timelineMode,
        mediaOptimizationConfigProvider = mediaOptimizationConfigProvider,
    )
}
