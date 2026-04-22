package com.example.myapplication.ui.screens.converter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.repository.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main converter screen showing list of currencies with editable values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEditList: () -> Unit,
    viewModel: ConverterViewModel = viewModel(factory = ConverterViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editingCurrencyCode by viewModel.editingCurrencyCode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Text(
                text = "V0.21",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, bottom = 8.dp),
                textAlign = TextAlign.End
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Sjors' Currency Converter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshRates() }) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    // Edit currency list
                    IconButton(onClick = onNavigateToEditList) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit list")
                    }
                    // Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show when rates were last updated
            if (uiState.lastUpdated != null) {
                Text(
                    text = "Rates from ${formatTimestamp(uiState.lastUpdated!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.isLoading && uiState.currencies.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Currency list with pull-to-refresh
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshRates() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.currencies,
                            key = { it.currency.code }
                        ) { currencyWithValue ->
                            CurrencyRow(
                                currencyWithValue = currencyWithValue,
                                decimalFormat = uiState.decimalFormat,
                                isEditing = editingCurrencyCode == currencyWithValue.currency.code,
                                onStartEditing = { viewModel.setEditingCurrency(currencyWithValue.currency.code) },
                                onStopEditing = { viewModel.setEditingCurrency(null) },
                                onValueChanged = { newValue ->
                                    viewModel.onValueChanged(currencyWithValue.currency.code, newValue)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single currency row with flag, code, and editable value.
 */
@Composable
private fun CurrencyRow(
    currencyWithValue: CurrencyWithValue,
    decimalFormat: DecimalFormat,
    isEditing: Boolean,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
    onValueChanged: (Double) -> Unit
) {
    val currency = currencyWithValue.currency
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Track tap state: first tap selects all, second tap places cursor
    var isFirstTap by remember { mutableStateOf(true) }

    // Store original text to detect actual changes
    var originalText by remember { mutableStateOf("") }
    var userHasEdited by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // Initialize text field when editing starts
    LaunchedEffect(isEditing) {
        if (isEditing) {
            val text = formatValueForEditing(currencyWithValue.value, decimalFormat)
            originalText = text
            userHasEdited = false
            textFieldValue = TextFieldValue(
                text = text,
                selection = TextRange(0, text.length)  // Select all on first tap
            )
            isFirstTap = true
            focusRequester.requestFocus()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartEditing() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag emoji
            Text(text = currency.flag, fontSize = 32.sp)

            Spacer(modifier = Modifier.width(12.dp))

            // Currency code
            Column(modifier = Modifier.width(60.dp)) {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Editable value
            Box(
                modifier = Modifier
                    .weight(2f)
                    .clickable {
                        if (isEditing && isFirstTap) {
                            // Second tap: place cursor at end
                            isFirstTap = false
                            textFieldValue = textFieldValue.copy(
                                selection = TextRange(textFieldValue.text.length)
                            )
                        }
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                BasicTextField(
                    value = if (isEditing) textFieldValue else TextFieldValue(formatValue(currencyWithValue.value, decimalFormat)),
                    onValueChange = { newValue ->
                        if (!isEditing) return@BasicTextField

                        // Filter to only digits and decimal separator
                        val filteredText = newValue.text.filter { char ->
                            char.isDigit() || char == decimalFormat.decimalSeparator
                        }

                        val textActuallyChanged = filteredText != textFieldValue.text
                        if (textActuallyChanged) {
                            isFirstTap = false
                            userHasEdited = true
                        }

                        textFieldValue = newValue.copy(text = filteredText)

                        // Only trigger conversion if user actually edited and text differs
                        if (userHasEdited && filteredText != originalText) {
                            parseValue(filteredText, decimalFormat)?.let { parsed ->
                                onValueChanged(parsed)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onStartEditing()
                            } else {
                                onStopEditing()
                            }
                        },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onStopEditing()
                        }
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    readOnly = !isEditing
                )
            }
        }
    }
}

/**
 * Formats value for display with thousands separators and 2 decimal places.
 */
private fun formatValue(value: Double, format: DecimalFormat): String {
    val wholePart = value.toLong()
    val decimalPart = ((value - wholePart) * 100).toLong()

    val wholePartFormatted = wholePart.toString()
        .reversed()
        .chunked(3)
        .joinToString(format.thousandsSeparator.toString())
        .reversed()

    return "$wholePartFormatted${format.decimalSeparator}${decimalPart.toString().padStart(2, '0')}"
}

/**
 * Formats value for editing (no thousands separator, minimal decimal places).
 */
private fun formatValueForEditing(value: Double, format: DecimalFormat): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value).replace('.', format.decimalSeparator)
    }
}

/**
 * Parses user input back to a Double.
 */
private fun parseValue(text: String, format: DecimalFormat): Double? {
    if (text.isBlank()) return 0.0
    val normalized = text
        .replace(format.thousandsSeparator.toString(), "")
        .replace(format.decimalSeparator, '.')
    return normalized.toDoubleOrNull()
}

/**
 * Formats timestamp as relative time (e.g., "5 minutes ago").
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
