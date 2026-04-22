package com.example.myapplication.ui.screens.editlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.domain.model.Currency
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditListViewModel = viewModel(factory = EditListViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Currency List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search currencies...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Text(
                text = "Tap to show/hide currencies. Hold and drag to reorder visible currencies.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val visibleCurrencies = uiState.currencies.filter { it.isVisible }
            val hiddenCurrencies = uiState.currencies.filter { !it.isVisible }

            var draggedItemIndex by remember { mutableIntStateOf(-1) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            val itemHeight = 72.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = rememberLazyListState()
            ) {
                // Visible currencies section (draggable)
                if (visibleCurrencies.isNotEmpty()) {
                    item {
                        Text(
                            text = "Visible",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    itemsIndexed(
                        items = visibleCurrencies,
                        key = { _, currency -> "visible_${currency.code}" }
                    ) { index, currency ->
                        val isDragging = draggedItemIndex == index
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "elevation"
                        )

                        CurrencyEditItem(
                            currency = currency,
                            onToggleVisibility = { viewModel.toggleCurrencyVisibility(currency) },
                            showDragHandle = true,
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .offset {
                                    IntOffset(
                                        0,
                                        if (isDragging) dragOffset.roundToInt() else 0
                                    )
                                }
                                .shadow(elevation, RoundedCornerShape(12.dp))
                                .pointerInput(visibleCurrencies) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedItemIndex = index
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y

                                            // Calculate target position
                                            val itemHeightPx = itemHeight.toPx() + 8.dp.toPx()
                                            val targetIndex =
                                                (index + (dragOffset / itemHeightPx).roundToInt())
                                                    .coerceIn(0, visibleCurrencies.size - 1)

                                            if (targetIndex != index && draggedItemIndex != -1) {
                                                viewModel.moveCurrency(index, targetIndex)
                                                draggedItemIndex = targetIndex
                                                dragOffset = 0f
                                            }
                                        },
                                        onDragEnd = {
                                            draggedItemIndex = -1
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedItemIndex = -1
                                            dragOffset = 0f
                                        }
                                    )
                                }
                        )
                    }
                }

                // Hidden currencies section
                if (hiddenCurrencies.isNotEmpty()) {
                    item {
                        Text(
                            text = "Hidden",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(
                        items = hiddenCurrencies,
                        key = { _, currency -> "hidden_${currency.code}" }
                    ) { _, currency ->
                        CurrencyEditItem(
                            currency = currency,
                            onToggleVisibility = { viewModel.toggleCurrencyVisibility(currency) },
                            showDragHandle = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyEditItem(
    currency: Currency,
    onToggleVisibility: () -> Unit,
    showDragHandle: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleVisibility() },
        colors = CardDefaults.cardColors(
            containerColor = if (currency.isVisible)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = currency.isVisible,
                onCheckedChange = { onToggleVisibility() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = currency.flag,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currency.isVisible)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = currency.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currency.isVisible)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            if (showDragHandle) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
