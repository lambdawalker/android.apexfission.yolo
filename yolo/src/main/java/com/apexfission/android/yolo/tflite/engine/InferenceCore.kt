package com.apexfission.android.yolo.tflite.engine

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.apexfission.android.yolo.engine.NumThreads
import com.apexfission.android.yolo.tflite.buffers.InputTensorBuffers
import com.apexfission.android.yolo.tflite.buffers.OutputTensorBuffers
import com.apexfission.android.yolo.tflite.validation.TensorContractValidator
import com.apexfission.android.yolo.tflite.validation.loadModelFile
import com.apexfission.android.yolo.tflite.validation.toMetadata

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

internal class InferenceCore(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default
) : InferenceEngine {

    companion object {
        private const val TAG = "InferenceCore"
    }

    private var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null
    private var isClosed = false

    internal val inputBuffers: InputTensorBuffers
    internal val outputBuffers: OutputTensorBuffers

    override val isInt8: Boolean get() = inputBuffers.isInt8
    override val inputImageWidth: Int get() = inputBuffers.imageWidth
    override val outLayout: InferenceEngine.OutputLayout get() = outputBuffers.layout
    override val outAttrs: Int get() = outputBuffers.attributes
    override val outBoxes: Int get() = outputBuffers.boxes
    override val numClasses: Int get() = outputBuffers.numClasses

    override var lastInferenceTimeMs: Long = 0L
        private set

    init {
        val modelBuffer = loadModelFile(context, modelPath)

        var temp: Interpreter? = null
        val contract = try {
            temp = Interpreter(modelBuffer)
            require(temp.inputTensorCount == 1) {
                "model must expose exactly 1 input tensor, but exposed ${temp.inputTensorCount}"
            }
            require(temp.outputTensorCount == 1) {
                "model must expose exactly 1 output tensor, but exposed ${temp.outputTensorCount}"
            }

            TensorContractValidator.validate(
                input = temp.getInputTensor(0).toMetadata(),
                output = temp.getOutputTensor(0).toMetadata(),
            )
        } catch (e: Exception) {
            throw TfliteInitializationException(
                "Model tensor contract is unsupported for '$modelPath': ${e.message}",
                e,
            )
        } finally {
            temp?.close()
        }

        Log.i(
            TAG,
            "Config: ${contract.outputAttributes - 4} classes, ${contract.outputBoxes} boxes, Layout: ${contract.outputLayout}, " +
                "Input INT8: ${contract.isInputInt8}, Output INT8: ${contract.isOutputInt8}",
        )

        val options = Interpreter.Options().apply {
            setNumThreads(numThreads.toInt())
        }

        inputBuffers = InputTensorBuffers.build(contract)
        outputBuffers = OutputTensorBuffers.build(contract)

        var localGpuDelegate: GpuDelegate? = null
        var localInterpreter: Interpreter? = null

        try {
            if (useGpu && !inputBuffers.isInt8) {
                try {
                    val compat = CompatibilityList()
                    if (compat.isDelegateSupportedOnThisDevice) {
                        localGpuDelegate = GpuDelegate(compat.bestOptionsForThisDevice)
                        options.addDelegate(localGpuDelegate)
                    }
                } catch (_: Throwable) {
                    localGpuDelegate?.close()
                    localGpuDelegate = null
                }
            }

            localInterpreter = Interpreter(modelBuffer, options)

            interpreter = localInterpreter
            gpuDelegate = localGpuDelegate
            localInterpreter = null
            localGpuDelegate = null
        } catch (e: Exception) {
            throw TfliteInitializationException("Failed to initialize TFLite interpreter for model: $modelPath", e)
        } finally {
            localInterpreter?.close()
            localGpuDelegate?.close()
        }
    }

    override fun runInference(bitmap: Bitmap): FloatArray {
        if (isClosed) return FloatArray(0)
        val startTime = SystemClock.uptimeMillis()

        inputBuffers.fill(bitmap)

        inputBuffers.buffer.rewind()
        interpreter.run(inputBuffers.buffer, outputBuffers.buffer)

        val resultFloats = outputBuffers.extractFloats()

        lastInferenceTimeMs = SystemClock.uptimeMillis() - startTime
        return resultFloats
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        try {
            interpreter.close()
        } finally {
            try {
                gpuDelegate?.close()
            } finally {
                gpuDelegate = null
            }
        }
    }
}
