package com.workoutlog.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.domain.model.WorkoutEntry
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.ui.components.DashStatCard
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.components.MonthYearPickerDialog
import com.workoutlog.ui.screens.entry.AddEditEntrySheet
import com.workoutlog.ui.screens.entry.EntryViewModel
import com.workoutlog.ui.screens.overview.MonthCalendar
import com.workoutlog.ui.theme.getWorkoutIcon
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    entryViewModel: EntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMonthPicker by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }
    var editGoalId by remember { mutableStateOf<Long?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var showEntrySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun openAddSheet(date: String?) {
        entryViewModel.setup(-1L, date ?: "")
        showEntrySheet = true
    }

    fun openEditSheet(entryId: Long) {
        entryViewModel.setup(entryId, "")
        showEntrySheet = true
    }

    val dragOffset = remember { Animatable(0f) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    fun animatePrevious() {
        scope.launch {
            dragOffset.animateTo(screenWidthPx, tween(150))
            viewModel.previousMonth()
            scrollState.scrollTo(0)
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    fun animateNext() {
        scope.launch {
            dragOffset.animateTo(-screenWidthPx, tween(150))
            viewModel.nextMonth()
            scrollState.scrollTo(0)
            dragOffset.snapTo(screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        if (state.isLoading) {
            LoadingIndicator()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header — title on left, filter + add buttons on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Workout Log",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (state.selectedFilters.isNotEmpty()) {
                                    Badge { Text(state.selectedFilters.size.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (state.selectedFilters.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { openAddSheet(null) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add workout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Month selector — static, does not animate with swipe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { animatePrevious() }) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showMonthPicker = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.currentMonth.format(DateTimeFormatter.ofPattern("MMMM")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.currentMonth.format(DateTimeFormatter.ofPattern("yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { animateNext() }) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Animated area: stat cards + goals + calendar slide together on month change
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { scope.launch { dragOffset.stop() } },
                            onDragEnd = {
                                scope.launch {
                                    when {
                                        dragOffset.value > 100 -> {
                                            dragOffset.animateTo(screenWidthPx, tween(150))
                                            viewModel.previousMonth()
                                            scrollState.scrollTo(0)
                                            dragOffset.snapTo(-screenWidthPx)
                                            dragOffset.animateTo(0f, tween(200))
                                        }
                                        dragOffset.value < -100 -> {
                                            dragOffset.animateTo(-screenWidthPx, tween(150))
                                            viewModel.nextMonth()
                                            scrollState.scrollTo(0)
                                            dragOffset.snapTo(screenWidthPx)
                                            dragOffset.animateTo(0f, tween(200))
                                        }
                                        else -> dragOffset.animateTo(0f, tween(150))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { dragOffset.animateTo(0f, tween(150)) }
                            },
                            onHorizontalDrag = { _, delta ->
                                scope.launch { dragOffset.snapTo(dragOffset.value + delta) }
                            }
                        )
                    }
            ) {
                Box(modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        // Stat cards
                        val hasData = state.daysElapsed > 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashStatCard(
                                icon = {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF6A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                accentColor = Color(0xFF4CAF6A),
                                label = "Workouts",
                                value = if (hasData) "${state.workoutCount} / ${state.daysElapsed}" else "—",
                                modifier = Modifier.weight(1f)
                            )
                            DashStatCard(
                                icon = {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Color(0xFF5B8DEE),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                accentColor = Color(0xFF5B8DEE),
                                label = "Consistency",
                                value = if (hasData) "${state.workoutPercentage}%" else "—",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Goals section — above calendar
                        GoalsSection(
                            goals = state.goals,
                            onManageClick = { editGoalId = null; showGoalSheet = true },
                            onGoalClick = { id -> editGoalId = id; showGoalSheet = true }
                        )

                        // Calendar grid — natural height via aspectRatio cells
                        MonthCalendar(
                            yearMonth = state.currentMonth,
                            entriesByDate = state.entriesByDate,
                            onDateClick = { date ->
                                val existing = state.entriesByDate[date]
                                if (!existing.isNullOrEmpty()) {
                                    openEditSheet(existing.first().id)
                                } else {
                                    openAddSheet(date.toString())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Month/year picker
    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = state.currentMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = { yearMonth ->
                if (yearMonth != state.currentMonth) {
                    val goingBack = yearMonth < state.currentMonth
                    scope.launch {
                        dragOffset.animateTo(if (goingBack) screenWidthPx else -screenWidthPx, tween(150))
                        viewModel.goToMonth(yearMonth)
                        dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                        dragOffset.animateTo(0f, tween(200))
                    }
                }
                showMonthPicker = false
            }
        )
    }

    // Entry form bottom sheet
    if (showEntrySheet) {
        ModalBottomSheet(
            onDismissRequest = { showEntrySheet = false },
            sheetState = sheetState,
            dragHandle = null,
            shape = RoundedCornerShape(0.dp),
            contentWindowInsets = { WindowInsets(0) }
        ) {
            AddEditEntrySheet(
                viewModel = entryViewModel,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showEntrySheet = false
                    }
                }
            )
        }
    }

    // Filter sheet
    if (showFilterSheet) {
        FilterSheet(
            workoutTypes = state.workoutTypes,
            selectedFilters = state.selectedFilters,
            onToggleFilter = { viewModel.toggleFilter(it) },
            onClearFilters = { viewModel.clearFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Goal management bottom sheet
    if (showGoalSheet) {
        GoalManagementSheet(
            goals = state.goals,
            workoutTypes = state.workoutTypes,
            onAddGoal = { period, target, typeId -> viewModel.addGoal(period, target, typeId) },
            onUpdateGoal = { id, period, target, typeId -> viewModel.updateGoal(id, period, target, typeId) },
            onDeleteGoal = { goalId -> viewModel.deleteGoal(goalId) },
            onDismiss = { showGoalSheet = false; editGoalId = null },
            initialEditGoalId = editGoalId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    workoutTypes: List<WorkoutType>,
    selectedFilters: Set<Long>,
    onToggleFilter: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nonRestTypes = workoutTypes.filter { !it.isRestDay }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Filter by Workout Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedFilters.isNotEmpty()) {
                    TextButton(onClick = onClearFilters) {
                        Text("Clear all")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nonRestTypes.forEach { type ->
                    val selected = type.id in selectedFilters
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleFilter(type.id) },
                        label = { Text(type.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(type.color)
                            )
                        }
                    )
                }
            }
            } // end CompositionLocalProvider
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEntryItem(
    entry: WorkoutEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            entry.workoutType?.color?.copy(alpha = 0.15f)
                                ?: MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getWorkoutIcon(entry.workoutType?.icon),
                        contentDescription = null,
                        tint = entry.workoutType?.color ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.workoutType?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.date.format(DateTimeFormatter.ofPattern("MMM d")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        entry.durationMinutes?.let {
                            Text(
                                text = " · ${it}min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        entry.caloriesBurned?.let {
                            Text(
                                text = " · ${it}cal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    entry.note?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 64.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
