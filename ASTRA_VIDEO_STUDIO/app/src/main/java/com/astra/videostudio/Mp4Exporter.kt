package com.astra.videostudio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

internal object Mp4Exporter {
    private const val W = 720
    private const val H = 1280
    private const val FPS = 30
    private const val PLANAR = 19
    private const val SEMIPLANAR = 21

    suspend fun export(context: Context, images: List<Uri>, secondsPerImage: Float): String =
        withContext(Dispatchers.IO) {
            require(images.isNotEmpty()) { "Нет изображений" }
            val mime = "video/avc"
            val codecInfo = MediaCodecListCompat.encoder(mime) ?: error("Не найден H.264 энкодер с YUV420")
            val color = codecInfo.getCapabilitiesForType(mime).colorFormats.first {
                it == PLANAR || it == SEMIPLANAR
            }
            val format = MediaFormat.createVideoFormat(mime, W, H).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, color)
                setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val codec = MediaCodec.createByCodecName(codecInfo.name)
            val temp = File(context.cacheDir, "astra_" + System.currentTimeMillis() + ".mp4")
            var muxer: MediaMuxer? = null
            var started = false
            var track = -1
            try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()
                muxer = MediaMuxer(temp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val info = MediaCodec.BufferInfo()
                val perImage = (secondsPerImage.coerceAtLeast(0.05f) * FPS).roundToInt().coerceAtLeast(1)
                val frameUs = 1_000_000L / FPS
                var frame = 0
                val total = images.size * perImage
                while (frame < total) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex) ?: error("Нет входного буфера")
                        input.clear()
                        val bitmap = render(context, images[frame / perImage])
                        val bytes = toYuv(bitmap, color == SEMIPLANAR)
                        bitmap.recycle()
                        require(bytes.size <= input.remaining()) { "Буфер кодека слишком мал" }
                        input.put(bytes)
                        codec.queueInputBuffer(inputIndex, 0, bytes.size, frame.toLong() * frameUs, 0)
                        frame++
                    }
                    val state = drain(codec, muxer, info, false, started, track)
                    started = state.first
                    track = state.second
                }
                var eos = false
                var done = false
                while (!done) {
                    if (!eos) {
                        val inputIndex = codec.dequeueInputBuffer(10_000)
                        if (inputIndex >= 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, total.toLong() * frameUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eos = true
                        }
                    }
                    val state = drain(codec, muxer, info, true, started, track)
                    started = state.first
                    track = state.second
                    done = state.third
                }
                check(started) { "Видеодорожка не создана" }
            } finally {
                runCatching { codec.stop() }
                codec.release()
                if (started) runCatching { muxer?.stop() }
                muxer?.release()
            }
            try {
                val name = "ASTRA_" + System.currentTimeMillis() + ".mp4"
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ASTRA")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Не удалось создать видео в галерее")
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        temp.inputStream().use { it.copyTo(output) }
                    } ?: error("Не удалось записать MP4")
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
                "Movies/ASTRA/" + name
            } finally {
                temp.delete()
            }
        }

    private fun drain(
        codec: MediaCodec, muxer: MediaMuxer, info: MediaCodec.BufferInfo,
        wait: Boolean, wasStarted: Boolean, oldTrack: Int
    ): Triple<Boolean, Int, Boolean> {
        var started = wasStarted
        var track = oldTrack
        var done = false
        var count = 0
        while (count++ < 50) {
            val index = codec.dequeueOutputBuffer(info, if (wait) 10_000 else 0)
            when (index) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> if (!wait) break
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!started) { "Повторное изменение формата" }
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    started = true
                }
                else -> if (index >= 0) {
                    val buffer = codec.getOutputBuffer(index)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0) {
                        check(started) { "Мультиплексор не запущен" }
                        val encoded = buffer ?: error("Пустой выходной буфер кодека")
                        encoded.position(info.offset)
                        encoded.limit(info.offset + info.size)
                        muxer.writeSampleData(track, encoded, info)
                    }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
                    codec.releaseOutputBuffer(index, false)
                    if (done) break
                }
            }
        }
        return Triple(started, track, done)
    }

    private fun render(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Повреждённое изображение" }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= W && bounds.outHeight / (sample * 2) >= H) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val source = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Не удалось декодировать изображение")
        val result = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(android.graphics.Color.BLACK)
        val scale = minOf(W.toFloat() / source.width, H.toFloat() / source.height)
        val width = source.width * scale
        val height = source.height * scale
        canvas.drawBitmap(source, null, RectF((W - width) / 2f, (H - height) / 2f, (W + width) / 2f, (H + height) / 2f), Paint(Paint.FILTER_BITMAP_FLAG))
        source.recycle()
        return result
    }

    private fun toYuv(bitmap: Bitmap, semi: Boolean): ByteArray {
        val pixels = IntArray(W * H)
        bitmap.getPixels(pixels, 0, W, 0, 0, W, H)
        val ySize = W * H
        val output = ByteArray(ySize + ySize / 2)
        var yi = 0
        var uv = ySize
        for (row in 0 until H) for (col in 0 until W) {
            val c = pixels[row * W + col]
            val r = c shr 16 and 255
            val g = c shr 8 and 255
            val b = c and 255
            output[yi++] = (((66*r + 129*g + 25*b + 128) shr 8) + 16).coerceIn(0,255).toByte()
            if (row % 2 == 0 && col % 2 == 0) {
                val u = (((-38*r - 74*g + 112*b + 128) shr 8) + 128).coerceIn(0,255).toByte()
                val v = (((112*r - 94*g - 18*b + 128) shr 8) + 128).coerceIn(0,255).toByte()
                if (semi) {
                    output[uv++] = u
                    output[uv++] = v
                } else {
                    val ci = (row / 2) * (W / 2) + col / 2
                    output[ySize + ci] = u
                    output[ySize + ySize / 4 + ci] = v
                }
            }
        }
        return output
    }
}

private object MediaCodecListCompat {
    fun encoder(mime: String): android.media.MediaCodecInfo? =
        android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, true) } &&
                info.getCapabilitiesForType(mime).colorFormats.any { it == 19 || it == 21 }
        }
}
