package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DualModeSwitch
import com.example.ui.components.RedactionCanvas
import com.example.ui.locale.AppStrings
import com.example.ui.viewmodel.ScanStep
import com.example.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    viewModel: ScanViewModel,
    accountMode: String,
    onModeChanged: (String) -> Unit,
    language: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentStep by viewModel.currentStep.collectAsState()
    val rawText by viewModel.rawOcrText.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val redactionBoxes by viewModel.redactionBoxes.collectAsState()
    val deleteImageAfterOcr by viewModel.deleteImageAfterOcr.collectAsState()
    val isAiParsed by viewModel.isAiParsed.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Form fields
    val merchant by viewModel.merchant.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val category by viewModel.category.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val date by viewModel.date.collectAsState()
    val time by viewModel.time.collectAsState()
    val invoiceNumber by viewModel.invoiceNumber.collectAsState()
    val tax by viewModel.tax.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val items by viewModel.items.collectAsState()

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.onPhotoCaptured(bitmap)
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImagePicked(context, uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.get("scan_receipt", language),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == ScanStep.SELECT_SOURCE || currentStep == ScanStep.COMPLETED) {
                            onNavigateBack()
                        } else {
                            viewModel.reset()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Account Mode Switcher
            DualModeSwitch(
                currentMode = accountMode,
                onModeChanged = onModeChanged,
                language = language
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (currentStep) {
                ScanStep.SELECT_SOURCE -> {
                    SourceSelectionView(
                        language = language,
                        onTakePhoto = { cameraLauncher.launch(null) },
                        onPickGallery = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                ScanStep.EXTRACTING_OCR -> {
                    LoadingStepView(
                        title = "Extracting Receipt Text",
                        description = AppStrings.get("extracting_text", language)
                    )
                }

                ScanStep.OCR_PREVIEW -> {
                    OcrPreviewAndRedactionView(
                        rawText = rawText,
                        bitmap = capturedBitmap,
                        redactionBoxes = redactionBoxes,
                        onBoxesChanged = { viewModel.setRedactionBoxes(it) },
                        deleteImage = deleteImageAfterOcr,
                        onDeleteImageChanged = { viewModel.setDeleteImageAfterOcr(it) },
                        onProceedSmartAi = { viewModel.proceedToSmartAiParse() },
                        onProceedOfflineRule = { viewModel.skipAiAndUseRuleBased() },
                        language = language
                    )
                }

                ScanStep.ANALYZING_AI -> {
                    LoadingStepView(
                        title = "Smart Parsing with Gemini",
                        description = AppStrings.get("analyzing_ai", language)
                    )
                }

                ScanStep.EDIT_DETAILS -> {
                    EditDetailsView(
                        merchant = merchant,
                        onMerchantChange = { viewModel.merchant.value = it },
                        totalAmount = totalAmount,
                        onTotalAmountChange = { viewModel.totalAmount.value = it },
                        currency = currency,
                        onCurrencyChange = { viewModel.currency.value = it },
                        category = category,
                        onCategoryChange = { viewModel.category.value = it },
                        paymentMethod = paymentMethod,
                        onPaymentMethodChange = { viewModel.paymentMethod.value = it },
                        date = date,
                        onDateChange = { viewModel.date.value = it },
                        time = time,
                        onTimeChange = { viewModel.time.value = it },
                        invoiceNumber = invoiceNumber,
                        onInvoiceNumberChange = { viewModel.invoiceNumber.value = it },
                        tax = tax,
                        onTaxChange = { viewModel.tax.value = it },
                        discount = discount,
                        onDiscountChange = { viewModel.discount.value = it },
                        items = items,
                        isAiParsed = isAiParsed,
                        language = language,
                        onSave = {
                            viewModel.saveTransaction(accountMode, displayedCanvasWidth = 360f, displayedCanvasHeight = 360f)
                        }
                    )
                }

                ScanStep.COMPLETED -> {
                    CompletedSuccessView(
                        language = language,
                        onScanAnother = { viewModel.reset() },
                        onGoHome = onNavigateBack
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SourceSelectionView(
    language: String,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Scan Any Receipt Offline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Google ML Kit OCR runs 100% on your device. No photos ever leave your phone without your permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Camera Action
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("take_photo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.get("take_photo", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gallery Action
                OutlinedButton(
                    onClick = onPickGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("choose_gallery_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.get("choose_gallery", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingStepView(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OcrPreviewAndRedactionView(
    rawText: String,
    bitmap: Bitmap?,
    redactionBoxes: List<androidx.compose.ui.geometry.Rect>,
    onBoxesChanged: (List<androidx.compose.ui.geometry.Rect>) -> Unit,
    deleteImage: Boolean,
    onDeleteImageChanged: (Boolean) -> Unit,
    onProceedSmartAi: () -> Unit,
    onProceedOfflineRule: () -> Unit,
    language: String
) {
    var showFullText by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Redaction Tool Card (if image is available)
        if (bitmap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get("privacy_redaction", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (redactionBoxes.isNotEmpty()) {
                            TextButton(
                                onClick = { onBoxesChanged(emptyList()) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Icon(Icons.Default.LayersClear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStrings.get("clear_redaction", language), fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        text = AppStrings.get("redact_instructions", language),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    RedactionCanvas(
                        bitmap = bitmap,
                        redactionBoxes = redactionBoxes,
                        onBoxesChanged = onBoxesChanged
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Privacy Mode: Delete image after OCR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = deleteImage,
                    onCheckedChange = onDeleteImageChanged,
                    modifier = Modifier.testTag("delete_image_checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.get("delete_image_toggle", language),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Raw OCR Text Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("raw_ocr_text", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { showFullText = !showFullText }) {
                        Text(if (showFullText) "Collapse" else "Expand")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (showFullText) rawText else rawText.take(240) + if (rawText.length > 240) "..." else "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action 1: Smart Gemini AI Parse (Sends ONLY OCR text)
        Button(
            onClick = onProceedSmartAi,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("smart_ai_parse_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Smart AI Parse (Gemini)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action 2: Offline Rule-Based Fallback
        OutlinedButton(
            onClick = onProceedOfflineRule,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("offline_fallback_parse_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Parse Offline (Fast Rule-Based)",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDetailsView(
    merchant: String,
    onMerchantChange: (String) -> Unit,
    totalAmount: String,
    onTotalAmountChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    paymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    invoiceNumber: String,
    onInvoiceNumberChange: (String) -> Unit,
    tax: String,
    onTaxChange: (String) -> Unit,
    discount: String,
    onDiscountChange: (String) -> Unit,
    items: List<com.example.data.local.entity.ReceiptItem>,
    isAiParsed: Boolean,
    language: String,
    onSave: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    val categoriesList = listOf("Food", "Groceries", "Fuel", "Medical", "Travel", "Shopping", "Utilities", "Entertainment", "Office", "Other")

    var paymentExpanded by remember { mutableStateOf(false) }
    val paymentsList = listOf("Cash", "Card", "UPI", "Online")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Parse status banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isAiParsed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAiParsed) Icons.Default.AutoAwesome else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (isAiParsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAiParsed) "Parsed by Gemini 1.5 Flash AI" else "Parsed by Offline Keyword Rules",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAiParsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Merchant Name
        OutlinedTextField(
            value = merchant,
            onValueChange = onMerchantChange,
            label = { Text(AppStrings.get("merchant_label", language)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_merchant"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Amount and Currency Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = currency,
                onValueChange = onCurrencyChange,
                label = { Text(AppStrings.get("currency_label", language)) },
                modifier = Modifier
                    .width(80.dp)
                    .testTag("input_currency"),
                singleLine = true
            )
            OutlinedTextField(
                value = totalAmount,
                onValueChange = onTotalAmountChange,
                label = { Text(AppStrings.get("amount_label", language)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_amount"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text(AppStrings.get("category_label", language)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("dropdown_category")
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categoriesList.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            onCategoryChange(cat)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Payment Method Dropdown
        ExposedDropdownMenuBox(
            expanded = paymentExpanded,
            onExpandedChange = { paymentExpanded = !paymentExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = paymentMethod,
                onValueChange = {},
                readOnly = true,
                label = { Text(AppStrings.get("payment_method", language)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("dropdown_payment_method")
            )
            ExposedDropdownMenu(
                expanded = paymentExpanded,
                onDismissRequest = { paymentExpanded = false }
            ) {
                paymentsList.forEach { pmt ->
                    DropdownMenuItem(
                        text = { Text(pmt) },
                        onClick = {
                            onPaymentMethodChange(pmt)
                            paymentExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Date and Time
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = date,
                onValueChange = onDateChange,
                label = { Text(AppStrings.get("date_label", language)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = time,
                onValueChange = onTimeChange,
                label = { Text("Time") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Invoice Number
        OutlinedTextField(
            value = invoiceNumber,
            onValueChange = onInvoiceNumberChange,
            label = { Text(AppStrings.get("invoice_label", language)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_expense_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = AppStrings.get("save_expense", language),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun CompletedSuccessView(
    language: String,
    onScanAnother: () -> Unit,
    onGoHome: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Expense Saved Successfully!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transaction saved in local Room database. 100% offline & private.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("go_to_dashboard_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Go to Dashboard",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onScanAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("scan_another_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Scan Another Receipt")
            }
        }
    }
}
