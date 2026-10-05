package com.apexfission.android.yolo.tflite.validation

import android.content.Context
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Loads a TFLite model file from the application assets folder into a direct [ByteBuffer].
 */
internal fun loadModelFile(context: Context, assetPath: String): ByteBuffer {
    return context.assets.openFd(assetPath).use { fd ->
        FileInputStream(fd.fileDescriptor).use { inputStream ->
            inputStream.channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
                ).order(ByteOrder.nativeOrder())
            }
        }
    }
}
