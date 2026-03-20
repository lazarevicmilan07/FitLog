package com.workoutlog.ui.screens.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.domain.model.MonthlyCountData
import com.workoutlog.domain.model.WorkoutTypeCountData
import com.workoutlog.ui.components.DashStatCard
import com.workoutlog.ui.components.EmptyState
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.components.MonthYearPickerDialog
import com.workoutlog.ui.components.YearPickerDialog
import androidx.compose.ui.res.stringResource
import com.workoutlog.R
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPeriodPicker by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    fun animatePrevious() {
        scope.launch {
            dragOffset.animateTo(screenWidthPx, tween(150))
            viewModel.previousPeriod()
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    fun animateNext() {
        scope.launch {
            dragOffset.animateTo(-screenWidthPx, tween(150))
            viewModel.nextPeriod()
            dragOffset.snapTo(screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with title + segmented tab control inline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.reports_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatsSegmentedControl(
                    isMonthly = state.isMonthly,
                    onSelect = { viewModel.setMonthly(it) }
                )
            }

            // Period selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { animatePrevious() }) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.cd_previous),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPeriodPicker = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.isMonthly)
                            YearMonth.of(state.selectedYear, state.selectedMonth)
                                .format(DateTimeFormatter.ofPattern("MMMM"))
                                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                        else
                            "${state.selectedYear}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.isMonthly) "${state.selectedYear}" else stringResource(R.string.reports_annual),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { animateNext() }) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Content with swipe gesture
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
                                            viewModel.previousPeriod()
                                            dragOffset.snapTo(-screenWidthPx)
                                            dragOffset.animateTo(0f, tween(200))
                                        }
                                        dragOffset.value < -100 -> {
                                            dragOffset.animateTo(-screenWidthPx, tween(150))
                                            viewModel.nextPeriod()
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
                    if (state.isLoading) {
                        LoadingIndicator()
                    } else if (state.isMonthly) {
                        MonthlyReportContent(state)
                    } else {
                        YearlyReportContent(state)
                    }
                }
            }
        }
    }

    if (showPeriodPicker) {
        if (state.isMonthly) {
            MonthYearPickerDialog(
                currentMonth = YearMonth.of(state.selectedYear, state.selectedMonth),
                onDismiss = { showPeriodPicker = false },
                onConfirm = { yearMonth ->
                    val currentYm = YearMonth.of(state.selectedYear, state.selectedMonth)
                    if (yearMonth != currentYm) {
                        val goingBack = yearMonth < currentYm
                        scope.launch {
                            dragOffset.animateTo(
                                if (goingBack) screenWidthPx else -screenWidthPx,
                                tween(150)
                            )
                            viewModel.goToPeriod(yearMonth.year, yearMonth.monthValue)
                            dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                            dragOffset.animateTo(0f, tween(200))
                        }
                    }
                    showPeriodPicker = false
                }
            )
        } else {
            YearPickerDialog(
                currentYear = state.selectedYear,
                onDismiss = { showPeriodPicker = false },
                onConfirm = { year ->
                    if (year != state.selectedYear) {
                        val goingBack = year < state.selectedYear
                        scope.launch {
                            dragOffset.animateTo(
                                if (goingBack) screenWidthPx else -screenWidthPx,
                                tween(150)
                            )
                            viewModel.setYear(year)
                            dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                            dragOffset.animateTo(0f, tween(200))
                        }
                    }
                    showPeriodPicker = false
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Monthly content
// ─────────────────────────────────────────────────────────

@Composable
fun MonthlyReportContent(state: ReportsUiState) {
    val report = state.monthlyReport

    if (report == null || report.totalWorkouts == 0) {
        EmptyState(title = stringResource(R.string.reports_no_workouts_month), subtitle = stringResource(R.string.reports_start_logging))
        return
    }

    var includeRestDays by remember { mutableStateOf(true) }
    var selectedDistIndex by remember(report, includeRestDays) { mutableStateOf(-1) }
    val filteredCounts = remember(report.workoutTypeCounts, includeRestDays) {
        if (includeRestDays) report.workoutTypeCounts
        else report.workoutTypeCounts.filter { !it.workoutType.isRestDay }
    }

    val ym = YearMonth.of(report.year, report.month)
    val currentYm = YearMonth.now()
    val daysElapsed = when {
        ym > currentYm -> 0
        ym == currentYm -> LocalDate.now().dayOfMonth
        else -> ym.lengthOfMonth()
    }
    val workoutCount = report.totalWorkouts - report.totalRestDays
    val workoutPercentage = if (daysElapsed > 0) workoutCount * 100 / daysElapsed else 0

    val favouriteWorkout = report.workoutTypeCounts
        .filter { !it.workoutType.isRestDay }
        .maxByOrNull { it.count }
        ?.workoutType?.name ?: "—"
    val purpleAccent = Color(0xFF8B5CF6)
    val amberAccent = MaterialTheme.colorScheme.tertiary

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2×2 dashboard-style stat cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        value = "$workoutCount / $daysElapsed",
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
                        value = "$workoutPercentage%",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.Hotel,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = Color(0xFFEF4444),
                        label = stringResource(R.string.overview_rest_days),
                        value = "${report.totalRestDays}",
                        modifier = Modifier.weight(1f)
                    )
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = amberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = amberAccent,
                        label = stringResource(R.string.reports_stat_favourite),
                        value = favouriteWorkout,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Workout distribution — donut + legend side by side
        if (report.workoutTypeCounts.isNotEmpty()) {
            item {
                StatsCard(title = stringResource(R.string.reports_stat_distribution)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: donut + toggle pill underneath
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (filteredCounts.isNotEmpty()) {
                                DonutChart(
                                    data = filteredCounts,
                                    selectedIndex = selectedDistIndex,
                                    onSelectionChange = { selectedDistIndex = it },
                                    modifier = Modifier.size(140.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .padding(top = if (filteredCounts.isNotEmpty()) 24.dp else 0.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { includeRestDays = !includeRestDays },
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.reports_rest_days_toggle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 34.dp, height = 18.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (includeRestDays)
                                                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1D4ED8) else Color(0xFF93C5FD)
                                            else if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1F2937) else Color(0xFFE5E7EB)
                                        )
                                        .padding(2.dp),
                                    contentAlignment = if (includeRestDays) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        // Right: legend or empty message
                        if (filteredCounts.isNotEmpty()) {
                            DistributionLegend(
                                data = filteredCounts,
                                selectedIndex = selectedDistIndex,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.reports_no_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Yearly content
// ─────────────────────────────────────────────────────────

@Composable
fun YearlyReportContent(state: ReportsUiState) {
    val report = state.yearlyReport

    if (report == null || report.totalWorkouts == 0) {
        EmptyState(title = stringResource(R.string.reports_no_workouts_year), subtitle = stringResource(R.string.reports_start_logging))
        return
    }

    val actualWorkouts = report.totalWorkouts - report.totalRestDays
    val currentYear = LocalDate.now().year
    val daysElapsed = when {
        report.year > currentYear -> 0
        report.year == currentYear -> LocalDate.now().dayOfYear
        else -> LocalDate.of(report.year, 12, 31).dayOfYear
    }
    val consistencyPct = if (daysElapsed > 0) actualWorkouts * 100 / daysElapsed else 0
    val elapsedMonths = when {
        report.year > currentYear -> 0
        report.year == currentYear -> LocalDate.now().monthValue
        else -> 12
    }
    val bestMonthData = report.monthlyCounts.maxByOrNull { it.count }
    val bestMonthValue = if (bestMonthData != null && bestMonthData.count > 0) {
        Month.of(bestMonthData.month).getDisplayName(JTextStyle.FULL, Locale.getDefault())
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    } else "—"
    val completedMonths = when {
        report.year > currentYear -> 0
        report.year == currentYear -> LocalDate.now().monthValue - 1
        else -> 12
    }
    val completedWorkouts = when {
        report.year == currentYear -> report.monthlyCounts
            .filter { it.month < LocalDate.now().monthValue }
            .sumOf { it.count }
        else -> actualWorkouts
    }
    val avgPerMonth = if (completedMonths > 0) (completedWorkouts.toFloat() / completedMonths).roundToInt() else 0
    val favouriteWorkout = report.workoutTypeCounts
        .filter { !it.workoutType.isRestDay }
        .maxByOrNull { it.count }
        ?.workoutType?.name ?: "—"
    val activeMonths = report.monthlyCounts.count { it.count > 0 }

    val greenAccent = Color(0xFF4CAF6A)
    val blueAccent = Color(0xFF5B8DEE)
    val amberAccent = MaterialTheme.colorScheme.tertiary
    val orangeAccent = Color(0xFFAB47BC)
    val violetAccent = Color(0xFF8B5CF6)
    val tealAccent = Color(0xFF26A69A)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 3×2 dash stat cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Workouts · Active Months
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = greenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = greenAccent,
                        label = stringResource(R.string.stat_workouts),
                        value = "$actualWorkouts / $daysElapsed",
                        modifier = Modifier.weight(1f),
                        labelMinLines = 2
                    )
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = tealAccent,
                        label = stringResource(R.string.reports_stat_active_months),
                        value = "$activeMonths / $elapsedMonths",
                        modifier = Modifier.weight(1f),
                        labelMinLines = 2
                    )
                }
                // Row 2: Best Month · Favourite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = violetAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = violetAccent,
                        label = stringResource(R.string.reports_stat_best_month),
                        value = bestMonthValue,
                        modifier = Modifier.weight(1f),
                        labelMinLines = 2
                    )
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = Color(0xFF29B6F6),
                        label = stringResource(R.string.reports_stat_favourite),
                        value = favouriteWorkout,
                        modifier = Modifier.weight(1f),
                        labelMinLines = 2
                    )
                }
                // Row 3: Avg/Month Consistency · Avg/Month Workouts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = blueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = blueAccent,
                        label = stringResource(R.string.reports_stat_avg_consistency),
                        value = "$consistencyPct%",
                        modifier = Modifier.weight(1f),
                        labelMinLines = 2
                    )
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = orangeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = orangeAccent,
                        label = stringResource(R.string.reports_stat_avg_workouts),
                        value = if (avgPerMonth > 0) "$avgPerMonth" else "—",
                        modifier = Modifier.weight(1f),
                        labelMinLines = 2
                    )
                }
            }
        }

        // Workout distribution — now first, matching monthly style with rest-days toggle
        if (report.workoutTypeCounts.isNotEmpty()) {
            item {
                var selectedDistIndex by remember(report) { mutableStateOf(-1) }
                var includeRestDays by remember { mutableStateOf(true) }
                val filteredCounts = remember(report.workoutTypeCounts, includeRestDays) {
                    if (includeRestDays) report.workoutTypeCounts
                    else report.workoutTypeCounts.filter { !it.workoutType.isRestDay }
                }
                StatsCard(title = stringResource(R.string.reports_stat_distribution)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (filteredCounts.isNotEmpty()) {
                                DonutChart(
                                    data = filteredCounts,
                                    selectedIndex = selectedDistIndex,
                                    onSelectionChange = { selectedDistIndex = it },
                                    modifier = Modifier.size(140.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .padding(top = if (filteredCounts.isNotEmpty()) 24.dp else 0.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { includeRestDays = !includeRestDays },
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.reports_rest_days_toggle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 34.dp, height = 18.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (includeRestDays)
                                                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1D4ED8) else Color(0xFF93C5FD)
                                            else if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1F2937) else Color(0xFFE5E7EB)
                                        )
                                        .padding(2.dp),
                                    contentAlignment = if (includeRestDays) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        if (filteredCounts.isNotEmpty()) {
                            DistributionLegend(
                                data = filteredCounts,
                                selectedIndex = selectedDistIndex,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.reports_no_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Monthly activity
        item {
            StatsCard(title = stringResource(R.string.reports_monthly_activity)) {
                MonthlyBarChart(
                    data = report.monthlyCounts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────

@Composable
fun StatsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun DonutChart(
    data: List<WorkoutTypeCountData>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val total = data.sumOf { it.count }
    val strokeWidth = 70f

    Box(
        modifier = modifier
            .clip(CircleShape)
            .pointerInput(data, selectedIndex) {
                detectTapGestures { offset ->
                    val canvasPx = minOf(size.width, size.height).toFloat()
                    val diameter = canvasPx - strokeWidth - 4f
                    val outerRadius = diameter / 2f + strokeWidth / 2f
                    val innerRadius = diameter / 2f - strokeWidth / 2f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    when {
                        dist < innerRadius -> onSelectionChange(-1)
                        dist <= outerRadius -> {
                            var angle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat() + 90f
                            if (angle < 0f) angle += 360f
                            var startAngle = 0f
                            var hit = -1
                            data.forEachIndexed { index, item ->
                                val sweep = (item.count / total.toFloat()) * 360f
                                if (angle >= startAngle && angle < startAngle + sweep) hit = index
                                startAngle += sweep
                            }
                            onSelectionChange(if (hit == selectedIndex) -1 else hit)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = minOf(size.width, size.height) - strokeWidth - 4f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f
            data.forEachIndexed { index, item ->
                val sweep = (item.count / total.toFloat()) * 360f
                val alpha = if (selectedIndex == -1 || selectedIndex == index) 1f else 0.2f
                drawArc(
                    color = item.workoutType.color.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        if (selectedIndex == -1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.reports_total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val selected = data[selectedIndex]
            val pct = (selected.count / total.toFloat() * 100).toInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${selected.count}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = selected.workoutType.color
                )
                Text(
                    text = "($pct%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = selected.workoutType.color
                )
            }
        }
    }
}

@Composable
fun DistributionLegend(
    data: List<WorkoutTypeCountData>,
    selectedIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.count }.toFloat()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            val dimmed = selectedIndex != -1 && !isSelected
            val contentAlpha = if (dimmed) 0.28f else 1f
            val fraction = item.count / total
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(item.workoutType.color.copy(alpha = contentAlpha))
                    )
                    Text(
                        text = item.workoutType.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Text(
                        text = "${item.count} (${(fraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
                // Mini progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(item.workoutType.color.copy(alpha = if (dimmed) 0.07f else 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(item.workoutType.color.copy(alpha = contentAlpha))
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    data: List<MonthlyCountData>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = data.maxOfOrNull { it.count }.takeIf { it != null && it > 0 } ?: 1
    val bestMonthNum = data.maxByOrNull { it.count }?.takeIf { it.count > 0 }?.month ?: -1
    val dataMap = data.associate { it.month to it.count }
    val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

    Canvas(modifier = modifier) {
        val labelAreaH = 36f
        val valueAreaH = 38f
        val chartH = size.height - labelAreaH - valueAreaH
        val barSlotW = size.width / 12f
        val barW = barSlotW * 0.55f

        // Subtle mid-line and baseline
        drawLine(
            color = gridColor,
            start = Offset(0f, valueAreaH + chartH * 0.5f),
            end = Offset(size.width, valueAreaH + chartH * 0.5f),
            strokeWidth = 1.5f
        )
        drawLine(
            color = gridColor.copy(alpha = 0.35f),
            start = Offset(0f, valueAreaH + chartH),
            end = Offset(size.width, valueAreaH + chartH),
            strokeWidth = 1.5f
        )

        val setColor: (android.graphics.Paint, Color) -> Unit = { paint, color ->
            paint.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        }

        val labelPaint = android.graphics.Paint().apply {
            textSize = 34f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val valuePaint = android.graphics.Paint().apply {
            textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        for (index in 0..11) {
            val month = index + 1
            val count = dataMap[month] ?: 0
            val xCenter = (index + 0.5f) * barSlotW
            val barLeft = index * barSlotW + (barSlotW - barW) / 2f
            val isBest = month == bestMonthNum

            if (count > 0) {
                val barH = (count.toFloat() / maxCount) * chartH
                val barTop = valueAreaH + chartH - barH
                val barColor = if (isBest) primary else primary.copy(alpha = 0.42f)
                val cornerR = CornerRadius(8f, 8f)

                // Full rounded rect, then square off bottom corners
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barW, barH),
                    cornerRadius = cornerR
                )
                val sqH = minOf(barH / 2f, 8f)
                drawRect(
                    color = barColor,
                    topLeft = Offset(barLeft, valueAreaH + chartH - sqH),
                    size = Size(barW, sqH)
                )

                // Count value above bar
                setColor(valuePaint, if (isBest) primary else labelColor)
                valuePaint.isFakeBoldText = isBest
                drawContext.canvas.nativeCanvas.drawText(
                    "$count",
                    xCenter,
                    barTop - 5f,
                    valuePaint
                )
            }

            // Month label
            setColor(labelPaint, if (isBest && count > 0) primary else labelColor)
            labelPaint.isFakeBoldText = isBest && count > 0
            drawContext.canvas.nativeCanvas.drawText(
                monthLabels[index],
                xCenter,
                size.height - 2f,
                labelPaint
            )
        }
    }
}

@Composable
private fun StatsSegmentedControl(
    isMonthly: Boolean,
    onSelect: (Boolean) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(surface)
            .padding(2.dp)
    ) {
        listOf(true to stringResource(R.string.reports_monthly), false to stringResource(R.string.reports_yearly)).forEach { (monthly, label) ->
            val selected = isMonthly == monthly
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) primary else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(monthly) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) onPrimary else onSurface
                )
            }
        }
    }
}

