package com.vaultstadio.app.accessibility

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Exercises the actual image decoder ABI selected alongside the Compose runtime. */
class ImageRuntimeCompatibilityTest {
    @Test
    fun realImagePipelineDecodesSyntheticPng() = runBlocking<Unit> {
        val loader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        try {
            val bytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg==",
            )
            val result = loader.execute(ImageRequest.Builder(PlatformContext.INSTANCE).data(bytes).build())
            val success = assertIs<SuccessResult>(result)
            assertEquals(1, success.image.width)
            assertEquals(1, success.image.height)
        } finally {
            loader.shutdown()
        }
    }

    @Test
    fun invalidImageReturnsAnExplicitError() = runBlocking<Unit> {
        val loader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        try {
            val result = loader.execute(
                ImageRequest.Builder(PlatformContext.INSTANCE).data("invalid image".encodeToByteArray()).build(),
            )
            assertIs<ErrorResult>(result)
        } finally {
            loader.shutdown()
        }
    }
}
