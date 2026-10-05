package com.apexfission.android.yolo.tflite.validation


import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Tensor

data class TensorMetadata(
    val shape: IntArray,
    val dataType: DataType,
    val quantizationScale: Float,
    val quantizationZeroPoint: Int,
)

internal fun Tensor.toMetadata(): TensorMetadata {
    val quantization = quantizationParams()
    return TensorMetadata(
        shape = shape(),
        dataType = dataType(),
        quantizationScale = quantization.scale,
        quantizationZeroPoint = quantization.zeroPoint,
    )
}

data class ModelTensorContract(
    val inputImageWidth: Int,
    val inputElementCount: Int,
    val inputByteCount: Int,
    val isInputInt8: Boolean,
    val inputQuantizationScale: Float,
    val inputQuantizationZeroPoint: Int,
    val outputElementCount: Int,
    val outputByteCount: Int,
    val isOutputInt8: Boolean,
    val outputQuantizationScale: Float,
    val outputQuantizationZeroPoint: Int,
    val outputLayout: InferenceEngine.OutputLayout,
    val outputAttributes: Int,
    val outputBoxes: Int,
)

object TensorContractValidator {
    private const val INPUT_CHANNELS = 3
    private const val MIN_OUTPUT_ATTRIBUTES = 5

    fun validate(input: TensorMetadata, output: TensorMetadata): ModelTensorContract {
        validateInputShape(input)
        validateDataType("input", input)
        validateOutputShape(output)
        validateDataType("output", output)

        val dim1 = output.shape[1]
        val dim2 = output.shape[2]
        require(dim1 != dim2) {
            "cannot determine YOLO output layout from shape ${output.shape.contentToString()}: " +
                "the attributes and boxes dimensions must be distinct"
        }

        val attributes = minOf(dim1, dim2)
        val boxes = maxOf(dim1, dim2)
        require(attributes >= MIN_OUTPUT_ATTRIBUTES) {
            "output tensor must provide at least $MIN_OUTPUT_ATTRIBUTES attributes " +
                "(4 coordinates and at least 1 class score), but shape was ${output.shape.contentToString()}"
        }

        val inputElementCount = checkedCount("input tensor element", input.shape, 1)
        val outputElementCount = checkedCount("output tensor element", output.shape, 1)
        val inputByteCount = checkedCount("input tensor byte", input.shape, bytesPerElement(input.dataType))
        val outputByteCount = checkedCount("output tensor byte", output.shape, bytesPerElement(output.dataType))

        return ModelTensorContract(
            inputImageWidth = input.shape[1],
            inputElementCount = inputElementCount,
            inputByteCount = inputByteCount,
            isInputInt8 = input.dataType == DataType.INT8,
            inputQuantizationScale = input.quantizationScale,
            inputQuantizationZeroPoint = input.quantizationZeroPoint,
            outputElementCount = outputElementCount,
            outputByteCount = outputByteCount,
            isOutputInt8 = output.dataType == DataType.INT8,
            outputQuantizationScale = output.quantizationScale,
            outputQuantizationZeroPoint = output.quantizationZeroPoint,
            outputLayout = if (dim1 == attributes) {
                InferenceEngine.OutputLayout.ATTRS_X_BOXES
            } else {
                InferenceEngine.OutputLayout.BOXES_X_ATTRS
            },
            outputAttributes = attributes,
            outputBoxes = boxes,
        )
    }

    private fun validateInputShape(input: TensorMetadata) {
        require(input.shape.size == 4 && input.shape[0] == 1 && input.shape[3] == INPUT_CHANNELS) {
            "input tensor must have shape [1, height, width, 3], but was ${input.shape.contentToString()}"
        }
        require(input.shape.all { it > 0 }) {
            "input tensor dimensions must be positive, but shape was ${input.shape.contentToString()}"
        }
        require(input.shape[1] == input.shape[2]) {
            "this detector requires a square input tensor, but shape was ${input.shape.contentToString()}"
        }
    }

    private fun validateOutputShape(output: TensorMetadata) {
        require(output.shape.size == 3 && output.shape[0] == 1) {
            "output tensor must have YOLO shape [1, attributes, boxes] or [1, boxes, attributes], " +
                "but was ${output.shape.contentToString()}"
        }
        require(output.shape.all { it > 0 }) {
            "output tensor dimensions must be positive, but shape was ${output.shape.contentToString()}"
        }
    }

    private fun validateDataType(name: String, tensor: TensorMetadata) {
        require(tensor.dataType == DataType.FLOAT32 || tensor.dataType == DataType.INT8) {
            "unsupported $name datatype ${tensor.dataType}; supported datatypes are FLOAT32 and INT8"
        }
        if (tensor.dataType == DataType.INT8) {
            require(tensor.quantizationScale.isFinite() && tensor.quantizationScale > 0f) {
                "$name INT8 tensor requires a finite positive quantization scale, " +
                    "but was ${tensor.quantizationScale}"
            }
            require(tensor.quantizationZeroPoint in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                "$name INT8 tensor zero point must be in ${Byte.MIN_VALUE}..${Byte.MAX_VALUE}, " +
                    "but was ${tensor.quantizationZeroPoint}"
            }
        }
    }

    private fun bytesPerElement(dataType: DataType): Int = when (dataType) {
        DataType.FLOAT32 -> Float.SIZE_BYTES
        DataType.INT8 -> Byte.SIZE_BYTES
        else -> error("datatype must be validated before allocation: $dataType")
    }

    private fun checkedCount(name: String, shape: IntArray, bytesPerElement: Int): Int {
        var count = bytesPerElement.toLong()
        for (dimension in shape) {
            if (count > Int.MAX_VALUE.toLong() / dimension) {
                throw IllegalArgumentException("$name count exceeds the maximum allocatable size for shape ${shape.contentToString()}")
            }
            count *= dimension
        }
        return count.toInt()
    }
}
