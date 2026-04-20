package com.workoutlog.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.domain.model.GoalPeriod
import com.workoutlog.domain.model.WorkoutEntry
import com.workoutlog.domain.model.WorkoutGoal
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.ui.components.DashStatCard
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.components.MonthYearPickerDialog
import com.workoutlog.ui.screens.entry.AddEditEntrySheet
import com.workoutlog.ui.screens.entry.EntryViewModel
import com.workoutlog.ui.screens.overview.MonthCalendar
import com.workoutlog.ui.theme.getWorkoutIcon
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.workoutlog.R

private data class GoalToastData(val goal: WorkoutGoal, val message: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    entryViewModel: EntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goalCompletedMessages = stringArrayResource(R.array.goal_completed_messages).toList()
    var goalToast by remember { mutableStateOf<GoalToastData?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }
    var editGoalId by remember { mutableStateOf<Long?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.goalCompletedEvent.collect { goal ->
            goalToast = GoalToastData(goal, goalCompletedMessages.random())
        }
    }

    LaunchedEffect(goalToast) {
        if (goalToast != null) {
            delay(3500)
            goalToast = null
        }
    }

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

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM") }
    val yearFormatter = remember { DateTimeFormatter.ofPattern("yyyy") }

    val dragOffset = remember { Animatable(0f) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    suspend fun doAnimatePrevious() {
        dragOffset.animateTo(screenWidthPx, tween(150))
        viewModel.previousMonth()
        scrollState.scrollTo(0)
        dragOffset.snapTo(-screenWidthPx)
        dragOffset.animateTo(0f, tween(200))
    }

    suspend fun doAnimateNext() {
        dragOffset.animateTo(-screenWidthPx, tween(150))
        viewModel.nextMonth()
        scrollState.scrollTo(0)
        dragOffset.snapTo(screenWidthPx)
        dragOffset.animateTo(0f, tween(200))
    }

    fun animatePrevious() { scope.launch { doAnimatePrevious() } }
    fun animateNext() { scope.launch { doAnimateNext() } }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = stringResource(R.string.home_title),
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
                                contentDescription = stringResource(R.string.cd_filter),
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
                            contentDescription = stringResource(R.string.cd_add_workout),
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
                        contentDescription = stringResource(R.string.cd_previous_month),
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
                        text = state.currentMonth.format(monthFormatter)
                            .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.currentMonth.format(yearFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { animateNext() }) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.cd_next_month),
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
                                        dragOffset.value > 100 -> doAnimatePrevious()
                                        dragOffset.value < -100 -> doAnimateNext()
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
                                label = stringResource(R.string.stat_workouts),
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
                                label = stringResource(R.string.stat_consistency),
                                value = if (hasData) "${state.workoutPercentage}%" else "—",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Goals section — above calendar (shown only if enabled in Goals screen)
                        if (state.showGoalsOnDashboard) {
                            GoalsSection(
                                goals = state.goals,
                                onManageClick = { editGoalId = null; showGoalSheet = true },
                                onGoalClick = { id -> editGoalId = id; showGoalSheet = true }
                            )
                        } else {
                            Spacer(Modifier.height(94.dp))
                        }

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
                            onOtherMonthClick = { date ->
                                val clickedMonth = YearMonth.from(date)
                                if (clickedMonth < state.currentMonth) scope.launch { doAnimatePrevious() }
                                else scope.launch { doAnimateNext() }
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

    // Goal completed toast overlay
    AnimatedVisibility(
        visible = goalToast != null,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { it } + fadeIn(tween(150)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
    ) {
        goalToast?.let { toast ->
            GoalCompletedToast(
                goal = toast.goal,
                message = toast.message,
                onDismiss = { goalToast = null }
            )
        }
    }

    } // end Box

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
            onAddGoal = { period, target, typeId, boundYM, showOnHome -> viewModel.addGoal(period, target, typeId, boundYM, showOnHome) },
            onUpdateGoal = { id, period, target, typeId -> viewModel.updateGoal(id, period, target, typeId) },
            onDeleteGoal = { goalId -> viewModel.deleteGoal(goalId) },
            onDismiss = { showGoalSheet = false; editGoalId = null },
            initialEditGoalId = editGoalId,
            initialBoundYearMonth = state.currentMonth
        )
    }
}

@Composable
private fun GoalCompletedToast(
    goal: WorkoutGoal,
    message: String,
    onDismiss: () -> Unit
) {
    val accentColor = when (goal.period) {
        GoalPeriod.MONTHLY -> Color(0xFF9C6ADE)
        GoalPeriod.YEARLY  -> Color(0xFFD4720A)
    }
    val periodLetter = when (goal.period) {
        GoalPeriod.MONTHLY -> "M"
        GoalPeriod.YEARLY  -> "Y"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp)
            .clickable(onClick = onDismiss),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Goal Complete!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = periodLetter,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            fontSize = 10.sp,
                            lineHeight = 10.sp
                        )
                    }
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            // Checkmark
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4A9B6F),
                modifier = Modifier.size(20.dp)
            )
        }
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.filter_by_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedFilters.isNotEmpty()) {
                    TextButton(onClick = onClearFilters) {
                        Text(stringResource(R.string.filter_clear_all))
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
    val entryDateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
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
                            text = entry.date.format(entryDateFormatter),
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
