/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import timber.log.Timber

/**
 * Edge length this loader decodes to.
 *
 * Everything it produces is consumed as a notification bitmap: the MediaStyle large icon, the
 * lock-screen media control, and the legacy session's `METADATA_KEY_ALBUM_ART`. `Notification`
 * scales large icons down to `notification_large_icon_width` (64dp) before posting — 256px even on
 * an xxxhdpi screen — so a larger decode is copied and then discarded by the framework.
 *
 * This is deliberately smaller than PLAYER_ARTWORK_SIZE: the player draws its artwork nearly
 * full-width, the notification never does. The disk-cache entry is keyed on the URL rather than
 * the decode size, so the two still share one download.
 */
private const val NOTIFICATION_ARTWORK_SIZE = 256

class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    private fun createFallbackBitmap(): Bitmap = createBitmap(64, 64)

    /**
     * Largest power-of-two subsample that still leaves both edges at or above
     * [NOTIFICATION_ARTWORK_SIZE]. Returns 1 when the bounds are unknown or already small enough.
     */
    private fun sampleSizeFor(
        width: Int,
        height: Int,
    ): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (width / (sample * 2) >= NOTIFICATION_ARTWORK_SIZE &&
            height / (sample * 2) >= NOTIFICATION_ARTWORK_SIZE
        ) {
            sample *= 2
        }
        return sample
    }

    private fun Bitmap.createIndependentCopy(): Bitmap {
        if (isRecycled) return createFallbackBitmap()
        return try {
            val copy = createBitmap(width, height)
            val canvas = android.graphics.Canvas(copy)
            canvas.drawBitmap(this, 0f, 0f, null)
            copy
        } catch (e: Exception) {
            Timber.tag("CoilBitmapLoader").w(e, "Failed to create independent copy")
            createFallbackBitmap()
        }
    }

    /**
     * Reached for browse items, whose `artworkData` is the raw encoded bytes of a cached artwork
     * file — full-size, whatever the source URL happened to be. Bounds are measured first so the
     * decode lands near [NOTIFICATION_ARTWORK_SIZE] instead of allocating the original dimensions
     * and then handing them straight to [createIndependentCopy], which doubles them.
     */
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            try {
                val bounds =
                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, bounds)

                val options =
                    BitmapFactory.Options().apply {
                        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
                    }
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                bitmap?.createIndependentCopy() ?: createFallbackBitmap()
            } catch (e: Exception) {
                Timber.tag("CoilBitmapLoader").w(e, "Failed to decode bitmap data")
                createFallbackBitmap()
            }
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            try {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(uri)
                        .size(NOTIFICATION_ARTWORK_SIZE)
                        .allowHardware(false)
                        .build()

                when (val result = context.imageLoader.execute(request)) {
                    is ErrorResult -> {
                        createFallbackBitmap()
                    }

                    is SuccessResult -> {
                        try {
                            val bitmap = result.image.toBitmap()
                            bitmap.createIndependentCopy()
                        } catch (e: Exception) {
                            Timber.tag("CoilBitmapLoader").w(e, "Failed to convert image to bitmap")
                            createFallbackBitmap()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("CoilBitmapLoader").w(e, "Failed to load bitmap from uri")
                createFallbackBitmap()
            }
        }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        metadata.artworkData?.let { return decodeBitmap(it) }
        val artworkUri = metadata.artworkUri ?: metadata.extras?.getString("artwork_uri")?.toUri() ?: return null
        return loadBitmap(artworkUri)
    }
}
