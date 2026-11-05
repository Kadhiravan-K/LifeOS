package com.example.domain.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import org.json.JSONArray
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class QrCodeType(val displayName: String, val fields: List<String>) {
    URL("Website", listOf("URL")),
    VCARD("Contact card", listOf("Name", "Phone", "Email", "Organization")),
    WIFI("Wi-Fi", listOf("Network name", "Password", "Security (WPA, WEP, or nopass)")),
    TEXT("Plain text", listOf("Text")),
    EMAIL("Email", listOf("Email address", "Subject", "Message")),
    PHONE("Phone", listOf("Phone number")),
    SMS("SMS", listOf("Phone number", "Message")),
    LOCATION("Location", listOf("Latitude", "Longitude", "Label"));

    companion object {
        fun fromStoredValue(value: String): QrCodeType =
            entries.firstOrNull { it.name == value } ?: TEXT
    }
}

data class QrCodeDraft(
    val title: String,
    val type: QrCodeType,
    val fields: List<String>
)

data class QrCodeTemplate(
    val id: String,
    val title: String,
    val description: String,
    val type: QrCodeType,
    val fields: List<String>
) {
    fun toDraft() = QrCodeDraft(title = title, type = type, fields = fields)
}

data class QrCodeImportSummary(
    val imported: Int,
    val alreadyPresent: Int
)

object QrCodeTemplateCatalog {
    val testingBundle: List<QrCodeTemplate> = listOf(
        QrCodeTemplate(
            id = "qr_sample_website",
            title = "Sample website",
            description = "Opens the reserved example.com website.",
            type = QrCodeType.URL,
            fields = listOf("https://example.com")
        ),
        QrCodeTemplate(
            id = "qr_sample_contact",
            title = "Sample contact",
            description = "Fictional vCard contact using reserved example details.",
            type = QrCodeType.VCARD,
            fields = listOf("Alex Example", "+1 202 555 0101", "alex@example.com", "LifeOS QA")
        ),
        QrCodeTemplate(
            id = "qr_sample_wifi",
            title = "Sample open Wi-Fi",
            description = "A test-only open network profile; it contains no password.",
            type = QrCodeType.WIFI,
            fields = listOf("LifeOS-QA-Guest", "", "nopass")
        ),
        QrCodeTemplate(
            id = "qr_sample_text",
            title = "Sample plain text",
            description = "A short offline text payload for scan testing.",
            type = QrCodeType.TEXT,
            fields = listOf("LifeOS QR scan test")
        ),
        QrCodeTemplate(
            id = "qr_sample_email",
            title = "Sample email",
            description = "A prefilled email to a reserved example address.",
            type = QrCodeType.EMAIL,
            fields = listOf("qa@example.com", "LifeOS QR test", "This is a sample message.")
        ),
        QrCodeTemplate(
            id = "qr_sample_phone",
            title = "Sample phone",
            description = "Uses a fictional number from the reserved 555-0100 range.",
            type = QrCodeType.PHONE,
            fields = listOf("+1 202 555 0102")
        ),
        QrCodeTemplate(
            id = "qr_sample_sms",
            title = "Sample SMS",
            description = "A prefilled SMS to a fictional reserved number.",
            type = QrCodeType.SMS,
            fields = listOf("+1 202 555 0103", "LifeOS QR scan test")
        ),
        QrCodeTemplate(
            id = "qr_sample_location",
            title = "Sample location",
            description = "A generic coordinate payload with a test label.",
            type = QrCodeType.LOCATION,
            fields = listOf("0", "0", "LifeOS QA sample")
        )
    )
}

data class QrCodeRecord(
    val id: Long = 0,
    val profileId: Long,
    val title: String,
    val type: QrCodeType,
    val fields: List<String>,
    val payload: String,
    val sourceTemplateId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

sealed interface QrCodeValidation {
    data class Valid(val payload: String) : QrCodeValidation
    data class Invalid(val message: String) : QrCodeValidation
}

object QrCodePayload {
    fun validateAndEncode(draft: QrCodeDraft): QrCodeValidation {
        if (draft.title.isBlank()) return QrCodeValidation.Invalid("Enter a name for this QR code.")
        if (draft.fields.size != draft.type.fields.size) {
            return QrCodeValidation.Invalid("Complete all fields for ${draft.type.displayName.lowercase()}.")
        }
        val values = draft.fields.map(String::trim)
        if (values.firstOrNull().isNullOrBlank()) {
            return QrCodeValidation.Invalid("${draft.type.fields.first()} is required.")
        }

        val payload = when (draft.type) {
            QrCodeType.URL -> {
                val uri = runCatching { URI(values[0]) }.getOrNull()
                if (uri == null ||
                    uri.scheme?.lowercase() !in setOf("http", "https") ||
                    uri.host.isNullOrBlank() ||
                    uri.rawUserInfo != null ||
                    (uri.port != -1 && uri.port !in 1..65535)
                ) {
                    return QrCodeValidation.Invalid("Enter a valid http or https URL.")
                }
                values[0]
            }
            QrCodeType.VCARD -> {
                if (values[1].isBlank() && values[2].isBlank()) {
                    return QrCodeValidation.Invalid("Add a phone number or email address.")
                }
                if (values[1].isNotBlank() && !isValidPhone(values[1])) {
                    return QrCodeValidation.Invalid("Enter a valid contact phone number.")
                }
                if (values[2].isNotBlank() && !isValidEmail(values[2])) {
                    return QrCodeValidation.Invalid("Enter a valid contact email address.")
                }
                buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("FN:${escapeVCard(values[0])}")
                    if (values[1].isNotBlank()) appendLine("TEL:${escapeVCard(values[1])}")
                    if (values[2].isNotBlank()) appendLine("EMAIL:${escapeVCard(values[2])}")
                    if (values[3].isNotBlank()) appendLine("ORG:${escapeVCard(values[3])}")
                    append("END:VCARD")
                }
            }
            QrCodeType.WIFI -> {
                val security = values[2].ifBlank { "WPA" }.uppercase()
                if (security !in setOf("WPA", "WEP", "NOPASS")) {
                    return QrCodeValidation.Invalid("Wi-Fi security must be WPA, WEP, or nopass.")
                }
                if (security != "NOPASS" && values[1].isBlank()) {
                    return QrCodeValidation.Invalid("Enter the Wi-Fi password or select nopass.")
                }
                "WIFI:T:$security;S:${escapeWifi(values[0])};P:${escapeWifi(values[1])};;"
            }
            QrCodeType.TEXT -> values[0]
            QrCodeType.EMAIL -> {
                if (!isValidEmail(values[0])) {
                    return QrCodeValidation.Invalid("Enter a valid email address.")
                }
                "mailto:${values[0]}?subject=${urlEncode(values[1])}&body=${urlEncode(values[2])}"
            }
            QrCodeType.PHONE -> {
                if (!isValidPhone(values[0])) return QrCodeValidation.Invalid("Enter a valid phone number.")
                "tel:${values[0].filter { it.isDigit() || it == '+' }}"
            }
            QrCodeType.SMS -> {
                if (!isValidPhone(values[0])) return QrCodeValidation.Invalid("Enter a valid phone number.")
                "SMSTO:${values[0].filter { it.isDigit() || it == '+' }}:${values[1]}"
            }
            QrCodeType.LOCATION -> {
                val latitude = values[0].toDoubleOrNull()
                val longitude = values[1].toDoubleOrNull()
                if (latitude == null || latitude !in -90.0..90.0 || longitude == null || longitude !in -180.0..180.0) {
                    return QrCodeValidation.Invalid("Enter valid latitude and longitude coordinates.")
                }
                val label = values[2].takeIf(String::isNotBlank)?.let { "?q=${urlEncode(it)}" }.orEmpty()
                "geo:$latitude,$longitude$label"
            }
        }
        return QrCodeValidation.Valid(payload)
    }

    fun encodeQr(payload: String, size: Int = 512): com.google.zxing.common.BitMatrix {
        require(size in 128..2048) { "QR image size must be between 128 and 2048 pixels." }
        return try {
            MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        } catch (error: WriterException) {
            throw IllegalArgumentException("The QR content is too large or could not be encoded.", error)
        }
    }

    fun fieldsToJson(fields: List<String>): String = JSONArray(fields).toString()

    fun fieldsFromJson(value: String, type: QrCodeType): List<String> {
        val array = runCatching { JSONArray(value) }.getOrNull()
        return type.fields.indices.map { index ->
            array?.optString(index).orEmpty()
        }
    }

    private fun escapeVCard(value: String): String =
        value.replace("\r\n", "\n").replace("\r", "\n").replace("\\", "\\\\").replace("\n", "\\n")
            .replace(",", "\\,").replace(";", "\\;")

    private fun escapeWifi(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:")
            .replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n")

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun isValidEmail(value: String): Boolean =
        value.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))

    private fun isValidPhone(value: String): Boolean =
        value.matches(Regex("^\\+?[0-9][0-9 ()-]{2,20}$")) &&
                value.count(Char::isDigit) in 7..15
}
