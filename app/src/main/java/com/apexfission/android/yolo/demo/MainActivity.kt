package com.apexfission.android.yolo.demo

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.buildYoloDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private var picture by mutableStateOf<Bitmap?>(null)
    private var detections by mutableStateOf(emptyList<Detection>())
    private var busy by mutableStateOf(false)
    private var status by mutableStateOf("Choose a photo to test the bundled card model, or run the post-processing example.")
    private var isExample by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var useGpu by remember { mutableStateOf(false) }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    if (uri != null) detectPhoto(uri, useGpu)
                }
                Scaffold { insets ->
                    Column(
                        Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Apexfission YOLO", style = MaterialTheme.typography.headlineMedium)
                        Text("Library demo · on-device card detection")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Request GPU (CPU fallback)")
                            Switch(checked = useGpu, onCheckedChange = { useGpu = it }, enabled = !busy)
                        }
                        Button(onClick = { picker.launch("image/*") }, enabled = !busy) { Text("Choose photo") }
                        OutlinedButton(onClick = ::runExample, enabled = !busy) { Text("Test post-processing") }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(status)
                        picture?.let { bitmap ->
                            Box(Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height)) {
                                Image(bitmap.asImageBitmap(), "Selected image with detection boxes", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                                Canvas(Modifier.fillMaxSize()) {
                                    val scaleX = size.width / bitmap.width
                                    val scaleY = size.height / bitmap.height
                                    detections.forEach { detection ->
                                        val box = detection.box
                                        drawRect(
                                            Color(0xFF00C853),
                                            Offset(box.x.toFloat() * scaleX, box.y.toFloat() * scaleY),
                                            Size((box.x2 - box.x).toFloat() * scaleX, (box.y2 - box.y).toFloat() * scaleY),
                                            style = Stroke(3.dp.toPx()),
                                        )
                                    }
                                }
                            }
                        }
                        detections.forEachIndexed { index, detection ->
                            val label = if (isExample) "Sample object" else DemoExamples.classLabels.getOrElse(detection.classId) { "Class ${detection.classId}" }
                            Text("${index + 1}. $label — ${String.format(Locale.ROOT, "%.1f", detection.confidence * 100)}%")
                        }
                        Text("Photos stay on this device. This demo does not verify identity or document authenticity.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    private fun runExample() {
        if (busy) return
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(235, 239, 245))
        }
        detections = DemoExamples.postProcess(bitmap)
        picture = bitmap
        isExample = true
        status = "Synthetic tensor: 3 candidates → ${detections.size} boxes after NMS (expected 2). This tests post-processing, not inference."
    }

    private fun detectPhoto(uri: Uri, useGpu: Boolean) {
        if (busy) return
        busy = true
        status = "Decoding photo and running inference…"
        detections = emptyList()
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val longest = max(info.size.width, info.size.height)
                        if (longest > 1600) {
                            val ratio = 1600f / longest
                            decoder.setTargetSize(max(1, (info.size.width * ratio).roundToInt()), max(1, (info.size.height * ratio).roundToInt()))
                        }
                    }
                    try {
                        // Construction, inference and close stay off the main thread.
                        // The factory confines native/GPU operations to its worker thread.
                        val start = System.nanoTime()
                        val found = buildYoloDetector(applicationContext, "demo.tflite", 0.35f, 0.45f, useGpu).use { it.detect(bitmap) }
                        Triple(bitmap, found, (System.nanoTime() - start) / 1_000_000)
                    } catch (failure: Throwable) {
                        bitmap.recycle()
                        throw failure
                    }
                }
                picture = result.first
                detections = result.second
                isExample = false
                status = "${result.second.size} detections in ${result.third} ms (includes model initialization)."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                status = "Could not analyze this photo: ${failure.message ?: failure.javaClass.simpleName}. Try another image or turn off GPU."
            } finally {
                busy = false
            }
        }
    }
}
