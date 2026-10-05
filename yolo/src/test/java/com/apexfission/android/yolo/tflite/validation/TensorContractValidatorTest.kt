package com.apexfission.android.yolo.tflite.validation

import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tensorflow.lite.DataType

class TensorContractValidatorTest {

    @Test
    fun `accepts mixed INT8 input and FLOAT32 attrs-first output`() {
        val contract = TensorContractValidator.validate(
            input = metadata(intArrayOf(1, 640, 640, 3), DataType.INT8, 1f / 255f, -128),
            output = metadata(intArrayOf(1, 5, 8400), DataType.FLOAT32),
        )

        assertTrue(contract.isInputInt8)
        assertFalse(contract.isOutputInt8)
        assertEquals(640, contract.inputImageWidth)
        assertEquals(5, contract.outputAttributes)
        assertEquals(8400, contract.outputBoxes)
        assertEquals(InferenceEngine.OutputLayout.ATTRS_X_BOXES, contract.outputLayout)
        assertEquals(1 * 640 * 640 * 3, contract.inputByteCount)
        assertEquals(1 * 5 * 8400 * 4, contract.outputByteCount)
    }

    @Test
    fun `accepts FLOAT32 input and INT8 boxes-first output`() {
        val contract = TensorContractValidator.validate(
            input = metadata(intArrayOf(1, 320, 320, 3), DataType.FLOAT32),
            output = metadata(intArrayOf(1, 2100, 6), DataType.INT8, 0.01f, 0),
        )

        assertFalse(contract.isInputInt8)
        assertTrue(contract.isOutputInt8)
        assertEquals(InferenceEngine.OutputLayout.BOXES_X_ATTRS, contract.outputLayout)
        assertEquals(6, contract.outputAttributes)
        assertEquals(2100, contract.outputBoxes)
    }

    @Test
    fun `rejects malformed input shape`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                metadata(intArrayOf(1, 640, 640), DataType.FLOAT32),
                validOutput(),
            )
        }

        assertTrue(error.message!!.contains("input tensor must have shape [1, height, width, 3]"))
    }

    @Test
    fun `rejects non-square input`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                metadata(intArrayOf(1, 320, 640, 3), DataType.FLOAT32),
                validOutput(),
            )
        }

        assertTrue(error.message!!.contains("square input"))
    }

    @Test
    fun `rejects UINT8 input`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                metadata(intArrayOf(1, 640, 640, 3), DataType.UINT8, 1f / 255f, 0),
                validOutput(),
            )
        }

        assertTrue(error.message!!.contains("unsupported input datatype UINT8"))
    }

    @Test
    fun `rejects UINT8 output`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                validInput(),
                metadata(intArrayOf(1, 5, 8400), DataType.UINT8, 0.01f, 0),
            )
        }

        assertTrue(error.message!!.contains("unsupported output datatype UINT8"))
    }

    @Test
    fun `rejects invalid INT8 quantization`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                metadata(intArrayOf(1, 640, 640, 3), DataType.INT8, 0f, 0),
                validOutput(),
            )
        }

        assertTrue(error.message!!.contains("finite positive quantization scale"))
    }

    @Test
    fun `rejects INT8 zero point outside datatype range`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                metadata(intArrayOf(1, 640, 640, 3), DataType.INT8, 0.01f, 128),
                validOutput(),
            )
        }

        assertTrue(error.message!!.contains("zero point must be in -128..127"))
    }

    @Test
    fun `rejects malformed output rank`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                validInput(),
                metadata(intArrayOf(1, 5, 80, 80), DataType.FLOAT32),
            )
        }

        assertTrue(error.message!!.contains("output tensor must have YOLO shape"))
    }

    @Test
    fun `rejects output with fewer than five attributes`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                validInput(),
                metadata(intArrayOf(1, 4, 8400), DataType.FLOAT32),
            )
        }

        assertTrue(error.message!!.contains("at least 5 attributes"))
    }

    @Test
    fun `rejects ambiguous output layout`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                validInput(),
                metadata(intArrayOf(1, 20, 20), DataType.FLOAT32),
            )
        }

        assertTrue(error.message!!.contains("cannot determine YOLO output layout"))
    }

    @Test
    fun `rejects byte buffer size overflow`() {
        val error = assertInvalid {
            TensorContractValidator.validate(
                validInput(),
                metadata(intArrayOf(1, 5, Int.MAX_VALUE), DataType.FLOAT32),
            )
        }

        assertTrue(error.message!!.contains("output tensor") && error.message!!.contains("count exceeds"))
    }

    private fun validInput() = metadata(intArrayOf(1, 640, 640, 3), DataType.FLOAT32)

    private fun validOutput() = metadata(intArrayOf(1, 5, 8400), DataType.FLOAT32)

    private fun metadata(
        shape: IntArray,
        dataType: DataType,
        scale: Float = 0f,
        zeroPoint: Int = 0,
    ) = TensorMetadata(shape, dataType, scale, zeroPoint)

    private fun assertInvalid(block: () -> Unit): IllegalArgumentException =
        assertThrows(IllegalArgumentException::class.java, block)
}
