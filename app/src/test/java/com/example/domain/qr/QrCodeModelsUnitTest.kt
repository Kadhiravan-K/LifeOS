package com.example.domain.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeModelsUnitTest {

    @Test
    fun testingBundleHasEightValidAndDecodableExamples() {
        val templates = QrCodeTemplateCatalog.testingBundle
        assertEquals(8, templates.size)
        assertEquals(QrCodeType.entries.toSet(), templates.map { it.type }.toSet())
        assertEquals(8, templates.map { it.id }.distinct().size)

        templates.forEach { template ->
            val payload = when (val result = QrCodePayload.validateAndEncode(template.toDraft())) {
                is QrCodeValidation.Valid -> result.payload
                is QrCodeValidation.Invalid -> error("${template.id}: ${result.message}")
            }
            val matrix = QrCodePayload.encodeQr(payload, 512)
            val pixels = IntArray(matrix.width * matrix.height) { index ->
                if (matrix[index % matrix.width, index / matrix.width]) 0xFF000000.toInt()
                else 0xFFFFFFFF.toInt()
            }
            val source = RGBLuminanceSource(matrix.width, matrix.height, pixels)
            val reader = MultiFormatReader().apply {
                setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
            }
            assertEquals(template.id, payload, reader.decode(BinaryBitmap(HybridBinarizer(source))).text)
        }
    }

    @Test
    fun invalidUrlEmailContactWifiAndLocationAreRejected() {
        assertInvalid(QrCodeDraft("bad url", QrCodeType.URL, listOf("javascript:alert(1)")))
        assertInvalid(QrCodeDraft("bad email", QrCodeType.EMAIL, listOf("not-an-email", "", "")))
        assertInvalid(QrCodeDraft("bad contact", QrCodeType.VCARD, listOf("Example", "", "invalid-email", "")))
        assertInvalid(QrCodeDraft("bad wifi", QrCodeType.WIFI, listOf("test", "", "WPA")))
        assertInvalid(QrCodeDraft("bad location", QrCodeType.LOCATION, listOf("91", "0", "")))
    }

    @Test
    fun testWifiSampleContainsNoPasswordAndNoTemplateContainsPersonalCredentials() {
        val wifi = QrCodeTemplateCatalog.testingBundle.single { it.type == QrCodeType.WIFI }
        assertEquals("nopass", wifi.fields[2])
        assertTrue(wifi.fields[1].isBlank())
        assertFalse(QrCodeTemplateCatalog.testingBundle.any { it.fields.any { field -> field.contains("password", true) } })
    }

    private fun assertInvalid(draft: QrCodeDraft) {
        assertTrue(QrCodePayload.validateAndEncode(draft) is QrCodeValidation.Invalid)
    }
}
