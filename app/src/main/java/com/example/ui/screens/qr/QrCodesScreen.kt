package com.example.ui.screens.qr

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.qr.QrCodeDraft
import com.example.domain.qr.QrCodePayload
import com.example.domain.qr.QrCodeRecord
import com.example.domain.qr.QrCodeType
import com.example.domain.qr.QrCodeValidation
import com.example.ui.screens.templates.TemplateBrowserDialog
import com.example.ui.screens.templates.TemplateBrowserTab
import com.example.ui.viewmodel.LifeViewModel
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrCodesScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val records by viewModel.qrCodes.collectAsStateWithLifecycle()
    var editingRecord by remember { mutableStateOf<QrCodeRecord?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var previewRecord by remember { mutableStateOf<QrCodeRecord?>(null) }
    var showBrowser by remember { mutableStateOf(false) }
    var scanRequested by remember { mutableStateOf(false) }
    var scanPermissionError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scannedContent by remember { mutableStateOf<String?>(null) }
    var downloadBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scanPermissionError = null
            scanRequested = true
        } else {
            scanPermissionError = "Camera permission is needed to scan a QR code."
        }
    }
    val downloadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bitmap = downloadBitmap
        downloadBitmap = null
        if (uri != null && bitmap != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        "The QR image could not be encoded."
                    }
                } ?: error("The selected file could not be opened.")
            }.onSuccess {
                message = "QR code downloaded."
            }.onFailure {
                errorMessage = "Could not download QR code: ${it.message ?: "unknown error"}"
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("QR Codes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Create and manage scannable codes on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showBrowser = true }) {
                    Icon(Icons.Default.QrCode2, contentDescription = "Browse QR templates")
                }
            }
        }
    ) { insets ->
        if (scanRequested) {
            QrScanner(
                modifier = Modifier.fillMaxSize().padding(insets),
                onDecoded = { content ->
                    scanRequested = false
                    previewRecord = null
                    scannedContent = content
                },
                onClose = { scanRequested = false },
                onError = {
                    scanRequested = false
                    errorMessage = it
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            editingRecord = null
                            showEditor = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Create QR")
                    }
                    OutlinedButton(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Scan")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showBrowser = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Browse QR Templates")
                }
                scanPermissionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                if (records.isEmpty()) {
                    EmptyQrState(
                        onCreate = {
                            editingRecord = null
                            showEditor = true
                        },
                        onBrowse = { showBrowser = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().testTag("qr_code_list"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(records, key = { it.id }) { record ->
                            QrRecordCard(
                                record = record,
                                onPreview = { previewRecord = record },
                                onEdit = {
                                    editingRecord = record
                                    showEditor = true
                                },
                                onShare = { shareQrImage(context, record) { errorMessage = it } },
                                onDownload = {
                                    runCatching { qrBitmap(record.payload) }
                                        .onSuccess {
                                            downloadBitmap = it
                                            downloadLauncher.launch("${safeFileName(record.title)}.png")
                                        }
                                        .onFailure { errorMessage = it.message ?: "Could not generate QR code." }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        QrCodeEditorDialog(
            record = editingRecord,
            onDismiss = { showEditor = false },
            onSave = { draft ->
                viewModel.saveQrCode(draft, editingRecord?.id) { result ->
                    result.onSuccess {
                        showEditor = false
                        message = if (editingRecord == null) "QR code saved." else "QR code updated."
                    }.onFailure {
                        errorMessage = it.message ?: "Could not save QR code."
                    }
                }
            }
        )
    }

    previewRecord?.let { record ->
        QrCodePreviewDialog(
            record = record,
            onDismiss = { previewRecord = null },
            onEdit = {
                editingRecord = record
                previewRecord = null
                showEditor = true
            },
            onShare = { shareQrImage(context, record) { errorMessage = it } },
            onDownload = {
                runCatching { qrBitmap(record.payload) }
                    .onSuccess {
                        downloadBitmap = it
                        downloadLauncher.launch("${safeFileName(record.title)}.png")
                    }
                    .onFailure { errorMessage = it.message ?: "Could not generate QR code." }
            }
        )
    }

    if (showBrowser) {
        val installedTemplates by viewModel.installedTemplateIds.collectAsStateWithLifecycle()
        val installedIds = remember(records) {
            records.mapNotNull { it.sourceTemplateId }.toSet()
        }
        TemplateBrowserDialog(
            installedTemplateIds = installedTemplates,
            onInstallTemplate = { template, tasks, habits, onDone ->
                viewModel.installTemplate(template, tasks, habits, onDone)
            },
            onImportQrTemplates = { templates, onDone ->
                viewModel.importQrTemplates(templates) { result ->
                    result.onSuccess { summary ->
                        onDone(summary.imported, summary.alreadyPresent)
                    }.onFailure {
                        errorMessage = it.message ?: "Could not import QR templates."
                    }
                }
            },
            installedQrTemplateIds = installedIds,
            onImportTrackerTemplate = { viewModel.saveTracker(it.toTrackerItem()) },
            onImportSubscriptionTemplate = { viewModel.saveSubscription(it.toSubscriptionItem()) },
            initialTab = TemplateBrowserTab.QR_CODES,
            onDismiss = { showBrowser = false }
        )
    }

    scannedContent?.let { content ->
        AlertDialog(
            onDismissRequest = { scannedContent = null },
            title = { Text("Scanned QR content") },
            text = { Text(content) },
            confirmButton = {
                TextButton(onClick = {
                    scannedContent = null
                    val decodedDraft = QrCodeDraft(
                        title = "Scanned QR content",
                        type = QrCodeType.TEXT,
                        fields = listOf(content)
                    )
                    viewModel.saveQrCode(decodedDraft) { result ->
                        result.onSuccess { message = "Scanned content saved." }
                            .onFailure { errorMessage = it.message ?: "Could not save scanned content." }
                    }
                }) { Text("Save as text QR") }
            },
            dismissButton = {
                TextButton(onClick = { scannedContent = null }) { Text("Close") }
            }
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("QR Codes") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }
        )
    }
    errorMessage?.let {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("QR Code Error") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun EmptyQrState(onCreate: () -> Unit, onBrowse: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("No QR codes yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Create a code or import the eight-code offline testing bundle.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreate) { Text("Create") }
            OutlinedButton(onClick = onBrowse) { Text("Browse templates") }
        }
    }
}

@Composable
private fun QrRecordCard(
    record: QrCodeRecord,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPreview).testTag("qr_code_${record.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            runCatching { qrBitmap(record.payload) }.getOrNull()?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR code for ${record.title}",
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(record.type.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(record.payload, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit ${record.title}") }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Share ${record.title}") }
            IconButton(onClick = onDownload) { Icon(Icons.Default.Download, contentDescription = "Download ${record.title}") }
        }
    }
}

@Composable
private fun QrCodeEditorDialog(
    record: QrCodeRecord?,
    onDismiss: () -> Unit,
    onSave: (QrCodeDraft) -> Unit
) {
    var selectedType by remember(record?.id) { mutableStateOf(record?.type ?: QrCodeType.URL) }
    var title by remember(record?.id) { mutableStateOf(record?.title.orEmpty()) }
    var fields by remember(record?.id, selectedType) {
        mutableStateOf(record?.takeIf { it.type == selectedType }?.fields ?: List(selectedType.fields.size) { "" })
    }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "Create QR Code" else "Edit QR Code") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedButton(onClick = { typeMenuExpanded = true }) {
                        Text(selectedType.displayName)
                    }
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        QrCodeType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                    validationMessage = null
                                }
                            )
                        }
                    }
                }
                selectedType.fields.forEachIndexed { index, label ->
                    OutlinedTextField(
                        value = fields.getOrElse(index) { "" },
                        onValueChange = { newValue ->
                            fields = fields.toMutableList().also { it[index] = newValue }
                        },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = label !in setOf("Text", "Message", "Subject")
                    )
                }
                if (selectedType == QrCodeType.WIFI) {
                    Text(
                        "Use NOPASS for a test-only open network. Never encode a private network password in sample data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val draft = QrCodeDraft(title, selectedType, fields)
                when (val result = QrCodePayload.validateAndEncode(draft)) {
                    is QrCodeValidation.Valid -> onSave(draft)
                    is QrCodeValidation.Invalid -> validationMessage = result.message
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QrCodePreviewDialog(
    record: QrCodeRecord,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    val bitmapResult = remember(record.payload) { runCatching { qrBitmap(record.payload) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                bitmapResult.onSuccess {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Scannable QR code for ${record.title}",
                        modifier = Modifier.size(240.dp).background(androidx.compose.ui.graphics.Color.White).padding(8.dp)
                    )
                }.onFailure {
                    Text("QR preview unavailable: ${it.message}", color = MaterialTheme.colorScheme.error)
                }
                Text(record.type.displayName, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
                Text(record.payload, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onShare) { Text("Share") }
                TextButton(onClick = onDownload) { Text("Download") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun QrScanner(
    onDecoded: (String) -> Unit,
    onClose: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val decodeReader = remember {
        MultiFormatReader().apply {
            setHints(EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
                put(DecodeHintType.TRY_HARDER, true)
            })
        }
    }
    val decoded = remember { AtomicBoolean(false) }

    Box(modifier = modifier) {
        AndroidViewQrPreview(
            modifier = Modifier.fillMaxSize(),
            lifecycleOwner = lifecycleOwner,
            onResult = { proxy ->
                if (!decoded.get()) {
                    try {
                        val source = proxy.toLuminanceSource()
                        val result = decodeReader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
                        if (decoded.compareAndSet(false, true)) mainExecutor.execute { onDecoded(result.text) }
                    } catch (_: ReaderException) {
                        Unit
                    } catch (error: Exception) {
                        if (decoded.compareAndSet(false, true)) {
                            mainExecutor.execute {
                                onError("Could not read the QR image: ${error.message ?: "invalid image"}")
                            }
                        }
                    } finally {
                        proxy.close()
                    }
                } else {
                    proxy.close()
                }
            },
            onError = onError
        )
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            TextButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null)
                Text("Close")
            }
        }
        Text(
            "Point the camera at a QR code",
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        )
    }
}

@Composable
private fun AndroidViewQrPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onResult: (ImageProxy) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val boundCamera = remember { mutableStateOf<Pair<ProcessCameraProvider, List<androidx.camera.core.UseCase>>?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val previewView = PreviewView(viewContext)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(viewContext)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor, onResult)
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    boundCamera.value = provider to listOf(preview, analysis)
                } catch (error: Exception) {
                    onError("Could not start the camera: ${error.message ?: "camera unavailable"}")
                }
            }, ContextCompat.getMainExecutor(viewContext))
            previewView
        }
    )
    DisposableEffect(context) {
        onDispose {
            boundCamera.value?.let { (provider, useCases) -> provider.unbind(*useCases.toTypedArray()) }
            executor.shutdown()
        }
    }
}

private fun ImageProxy.toLuminanceSource(): LuminanceSource {
    val plane = planes.firstOrNull() ?: error("The camera did not provide image data.")
    val yBuffer = plane.buffer
    val yBytes = ByteArray(width * height)
    for (row in 0 until height) {
        for (column in 0 until width) {
            yBytes[row * width + column] =
                yBuffer.get(row * plane.rowStride + column * plane.pixelStride)
        }
    }
    val source = PlanarYUVLuminanceSource(yBytes, width, height, 0, 0, width, height, false)
    return when (imageInfo.rotationDegrees) {
        90 -> source.rotateCounterClockwise()
            .rotateCounterClockwise()
            .rotateCounterClockwise()
        180 -> source.rotateCounterClockwise().rotateCounterClockwise()
        270 -> source.rotateCounterClockwise()
        else -> source
    }
}

private fun qrBitmap(payload: String): Bitmap {
    val matrix = QrCodePayload.encodeQr(payload, 512)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        val x = index % matrix.width
        val y = index / matrix.width
        if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}

private fun shareQrImage(context: android.content.Context, record: QrCodeRecord, onError: (String) -> Unit) {
    runCatching {
        val directory = File(context.cacheDir, "shared_qr")
        check(directory.exists() || directory.mkdirs()) { "Could not create a temporary QR image." }
        val image = File(directory, "${safeFileName(record.title)}.png")
        FileOutputStream(image).use { output ->
            check(qrBitmap(record.payload).compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "The QR image could not be encoded."
            }
        }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, record.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR code"))
    }.onFailure { onError("Could not share QR code: ${it.message ?: "unknown error"}") }
}

private fun safeFileName(value: String): String =
    value.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "lifeos_qr" }
