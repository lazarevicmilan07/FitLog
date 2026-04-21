package com.workoutlog.ui.screens.goals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.R
import com.workoutlog.domain.model.GoalPeriod
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.components.YearPickerDialog
import com.workoutlog.ui.screens.home.GoalManagementSheet

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showGoalSheet by remember { mutableStateOf(false) }
    var editGoalId by remember { mutableStateOf<Long?>(null) }
    var isMonthlyCollapsed by remember { mutableStateOf(false) }
    var isYearlyCollapsed by remember { mutableStateOf(false) }
    var isHistoryCollapsed by remember { mutableStateOf(false) }
    var showMonthlyYearPicker by remember { mutableStateOf(false) }
    var showYearlyYearPicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val monthlyDragOffset = remember { Animatable(0f) }
    val yearlyDragOffset = remember { Animatable(0f) }

    suspend fun doMonthlyPrev() {
        monthlyDragOffset.animateTo(screenWidthPx, tween(150))
        viewModel.previousMonthlyYear()
        monthlyDragOffset.snapTo(-screenWidthPx)
        monthlyDragOffset.animateTo(0f, tween(200))
    }
    suspend fun doMonthlyNext() {
        monthlyDragOffset.animateTo(-screenWidthPx, tween(150))
        viewModel.nextMonthlyYear()
        monthlyDragOffset.snapTo(screenWidthPx)
        monthlyDragOffset.animateTo(0f, tween(200))
    }
    suspend fun doYearlyPrev() {
        yearlyDragOffset.animateTo(screenWidthPx, tween(150))
        viewModel.previousYearlyPeriod()
        yearlyDragOffset.snapTo(-screenWidthPx)
        yearlyDragOffset.animateTo(0f, tween(200))
    }
    suspend fun doYearlyNext() {
        yearlyDragOffset.animateTo(-screenWidthPx, tween(150))
        viewModel.nextYearlyPeriod()
        yearlyDragOffset.snapTo(screenWidthPx)
        yearlyDragOffset.animateTo(0f, tween(200))
    }
    suspend fun animateMonthlyTo(year: Int) {
        val forward = year > state.selectedMonthlyYear
        monthlyDragOffset.animateTo(if (forward) -screenWidthPx else screenWidthPx, tween(150))
        viewModel.goToMonthlyYear(year)
        monthlyDragOffset.snapTo(if (forward) screenWidthPx else -screenWidthPx)
        monthlyDragOffset.animateTo(0f, tween(200))
    }
    suspend fun animateYearlyTo(year: Int) {
        val currentMid = state.yearlyPeriodStart + 2
        val forward = year > currentMid
        yearlyDragOffset.animateTo(if (forward) -screenWidthPx else screenWidthPx, tween(150))
        viewModel.goToYearlyPeriod(year)
        yearlyDragOffset.snapTo(if (forward) screenWidthPx else -screenWidthPx)
        yearlyDragOffset.animateTo(0f, tween(200))
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.goals_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { editGoalId = null; showGoalSheet = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_manage_goals),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))

                // Monthly Goals section — collapsible
                CollapsibleSectionHeader(
                    text = stringResource(R.string.goals_period_monthly) + " " + stringResource(R.string.goals_screen_title),
                    isCollapsed = isMonthlyCollapsed,
                    onToggle = { isMonthlyCollapsed = !isMonthlyCollapsed }
                )

                if (!isMonthlyCollapsed) {
                    Spacer(Modifier.height(8.dp))

                    YearSelector(
                        year = state.selectedMonthlyYear,
                        onPrevious = { scope.launch { doMonthlyPrev() } },
                        onNext = { scope.launch { doMonthlyNext() } },
                        onYearClick = { showMonthlyYearPicker = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { scope.launch { monthlyDragOffset.stop() } },
                                    onDragEnd = {
                                        scope.launch {
                                            when {
                                                monthlyDragOffset.value > 100 -> doMonthlyPrev()
                                                monthlyDragOffset.value < -100 -> doMonthlyNext()
                                                else -> monthlyDragOffset.animateTo(0f, tween(150))
                                            }
                                        }
                                    },
                                    onDragCancel = { scope.launch { monthlyDragOffset.animateTo(0f, tween(150)) } },
                                    onHorizontalDrag = { _, delta ->
                                        scope.launch { monthlyDragOffset.snapTo(monthlyDragOffset.value + delta) }
                                    }
                                )
                            }
                    ) {
                        Box(modifier = Modifier.offset { IntOffset(monthlyDragOffset.value.roundToInt(), 0) }) {
                            if (state.monthlyGoalGroups.isEmpty()) {
                                EmptyGoalsHint(
                                    text = stringResource(R.string.goals_empty_active),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    state.monthlyGoalGroups.forEach { group ->
                                        MonthlyGoalTypeCard(
                                            group = group,
                                            onToggleDashboard = { id, show -> viewModel.setGoalShowOnDashboard(id, show) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Yearly Goals section — collapsible
                CollapsibleSectionHeader(
                    text = stringResource(R.string.goals_period_yearly) + " " + stringResource(R.string.goals_screen_title),
                    isCollapsed = isYearlyCollapsed,
                    onToggle = { isYearlyCollapsed = !isYearlyCollapsed }
                )

                if (!isYearlyCollapsed) {
                    Spacer(Modifier.height(8.dp))

                    PeriodSelector(
                        periodStart = state.yearlyPeriodStart,
                        periodEnd = state.yearlyPeriodStart + 5,
                        onPrevious = { scope.launch { doYearlyPrev() } },
                        onNext = { scope.launch { doYearlyNext() } },
                        onPeriodClick = { showYearlyYearPicker = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { scope.launch { yearlyDragOffset.stop() } },
                                    onDragEnd = {
                                        scope.launch {
                                            when {
                                                yearlyDragOffset.value > 100 -> doYearlyPrev()
                                                yearlyDragOffset.value < -100 -> doYearlyNext()
                                                else -> yearlyDragOffset.animateTo(0f, tween(150))
                                            }
                                        }
                                    },
                                    onDragCancel = { scope.launch { yearlyDragOffset.animateTo(0f, tween(150)) } },
                                    onHorizontalDrag = { _, delta ->
                                        scope.launch { yearlyDragOffset.snapTo(yearlyDragOffset.value + delta) }
                                    }
                                )
                            }
                    ) {
                        Box(modifier = Modifier.offset { IntOffset(yearlyDragOffset.value.roundToInt(), 0) }) {
                            if (state.yearlyGoalGroups.isEmpty()) {
                                EmptyGoalsHint(
                                    text = stringResource(R.string.goals_empty_active),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    state.yearlyGoalGroups.forEach { group ->
                                        YearlyGoalTypeCard(
                                            group = group,
                                            onToggleDashboard = { id, show -> viewModel.setGoalShowOnDashboard(id, show) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // History section — collapsible
                CollapsibleSectionHeader(
                    text = stringResource(R.string.goals_section_history),
                    isCollapsed = isHistoryCollapsed,
                    onToggle = { isHistoryCollapsed = !isHistoryCollapsed }
                )

                if (!isHistoryCollapsed) {
                    Spacer(Modifier.height(8.dp))

                    if (state.historyYearGroups.isEmpty()) {
                        EmptyGoalsHint(
                            text = stringResource(R.string.goals_empty_history),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        GoalHistoryList(
                            historyYearGroups = state.historyYearGroups,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showGoalSheet) {
        GoalManagementSheet(
            goals = state.allGoals,
            workoutTypes = state.workoutTypes,
            onAddGoal = { period, target, typeId, boundYM, showOnHome -> viewModel.addGoal(period, target, typeId, boundYM, showOnHome) },
            onUpdateGoal = { id, period, target, typeId, boundYM, showOnHome -> viewModel.updateGoal(id, period, target, typeId, boundYM, showOnHome) },
            onDeleteGoal = { goalId -> viewModel.deleteGoal(goalId) },
            onDismiss = { showGoalSheet = false; editGoalId = null },
            initialEditGoalId = editGoalId
        )
    }

    if (showMonthlyYearPicker) {
        YearPickerDialog(
            currentYear = state.selectedMonthlyYear,
            onDismiss = { showMonthlyYearPicker = false },
            onConfirm = { year ->
                showMonthlyYearPicker = false
                if (year != state.selectedMonthlyYear) {
                    scope.launch { animateMonthlyTo(year) }
                }
            }
        )
    }

    if (showYearlyYearPicker) {
        YearPickerDialog(
            currentYear = state.yearlyPeriodStart + 5,
            onDismiss = { showYearlyYearPicker = false },
            onConfirm = { year ->
                showYearlyYearPicker = false
                val targetPeriodStart = year - 5
                if (targetPeriodStart != state.yearlyPeriodStart) {
                    scope.launch { animateYearlyTo(year) }
                }
            }
        )
    }
}

@Composable
private fun CollapsibleSectionHeader(
    text: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        )
        Icon(
            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun YearSelector(
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onYearClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(onClick = onYearClick)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PeriodSelector(
    periodStart: Int,
    periodEnd: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPeriodClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(onClick = onPeriodClick)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$periodStart – $periodEnd",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyGoalsHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun GoalHistoryList(
    historyYearGroups: List<HistoryYearGroup>,
    modifier: Modifier = Modifier
) {
    val collapsedYears = remember { mutableStateMapOf<Int, Boolean>() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        historyYearGroups.forEach { yearGroup ->
            val isYearCollapsed = collapsedYears[yearGroup.year] ?: false

            // Year header — prominent accent-strip row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { collapsedYears[yearGroup.year] = !isYearCollapsed },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = yearGroup.year.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
                Icon(
                    imageVector = if (isYearCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            if (!isYearCollapsed) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    yearGroup.periodGroups.forEach { periodGroup ->
                        val periodColor = periodGroup.period.accentColor()

                        // Period card with colored left strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, periodColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .background(periodColor.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(periodColor.copy(alpha = 0.7f))
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(if (periodGroup.period == GoalPeriod.MONTHLY) R.string.goals_period_monthly else R.string.goals_period_yearly),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = periodColor
                                )
                                periodGroup.monthGroups.forEach { monthGroup ->
                                    if (monthGroup.monthLabel.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = monthGroup.monthLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            )
                                        }
                                    }
                                    monthGroup.items.forEachIndexed { itemIndex, item ->
                                        HistoryRow(item = item)
                                        if (itemIndex < monthGroup.items.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                                modifier = Modifier.padding(start = 32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}


@Composable
private fun HistoryStatusDot(
    background: Color,
    icon: ImageVector,
    iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun HistoryRow(item: PastGoalPeriod) {
    val accentColor = item.goal.period.accentColor()
    val allWorkoutsLabel = stringResource(R.string.goals_all_workouts)
    val statusColor = when {
        item.isInProgress -> InProgressColor
        item.isHit        -> HitColor
        else              -> MissColor
    }
    val pct = if (item.goal.targetCount > 0) item.achieved * 100 / item.goal.targetCount else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            item.isInProgress -> HistoryStatusDot(InProgressColor, Icons.Default.MoreHoriz, Color.Black.copy(alpha = 0.75f))
            item.isHit        -> HistoryStatusDot(HitColor,        Icons.Default.Check,     Color.White)
            else              -> HistoryStatusDot(MissColor,        Icons.Default.Close,     Color.White)
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = item.goal.workoutType?.name ?: allWorkoutsLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = accentColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${item.achieved}/${item.goal.targetCount} · $pct%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}
