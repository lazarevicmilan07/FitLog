package com.workoutlog.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.workoutlog.R
import com.workoutlog.domain.model.GoalPeriod
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.ui.components.MonthYearPickerDialog
import com.workoutlog.ui.components.YearPickerDialog
import com.workoutlog.ui.screens.goals.HitColor
import com.workoutlog.ui.screens.goals.MissColor
import com.workoutlog.ui.screens.goals.accentColor
import com.workoutlog.ui.screens.goals.letter
import java.time.YearMonth
import java.time.format.DateTimeFormatter


@Composable
fun GoalsSection(
    goals: List<GoalWithProgress>,
    onManageClick: () -> Unit,
    onGoalClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.goals_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            IconButton(
                onClick = onManageClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_manage_goals),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (goals.isEmpty()) {
            Text(
                text = stringResource(R.string.goals_empty),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                goals.forEach { gp ->
                    GoalProgressCard(
                        goalProgress = gp,
                        onClick = { onGoalClick(gp.goal.id) }
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
fun GoalProgressCard(
    goalProgress: GoalWithProgress,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val goal = goalProgress.goal
    val progress = if (goal.targetCount > 0) {
        goalProgress.current.toFloat() / goal.targetCount.toFloat()
    } else 0f
    val isComplete = progress >= 1f
    val accentColor = goal.period.accentColor()
    val allWorkoutsLabel = stringResource(R.string.goals_all_workouts)
    val typeName = goal.workoutType?.name ?: allWorkoutsLabel
    val pct = (progress.coerceIn(0f, 1f) * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        // Top row: period badge + type name (accent label) + count value — matches DashStatCard
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(accentColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = goal.period.letter(),
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontSize = 11.sp,
                    lineHeight = 11.sp
                )
            }
            Spacer(Modifier.width(7.dp))
            Text(
                text = "${stringResource(when (goal.period) { GoalPeriod.MONTHLY -> R.string.goals_period_monthly; GoalPeriod.YEARLY -> R.string.goals_period_yearly })} · $typeName",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${goalProgress.current} / ${goal.targetCount}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(6.dp))

        // Single-block progress bar with percentage / checkmark at end
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .background(accentColor)
                )
            }
            Spacer(Modifier.width(7.dp))
            when {
                isComplete -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.cd_goal_completed),
                    tint = HitColor,
                    modifier = Modifier.size(14.dp)
                )
                goalProgress.isPast -> Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = stringResource(R.string.cd_goal_not_completed),
                    tint = MissColor,
                    modifier = Modifier.size(14.dp)
                )
                else -> Text(
                    text = "$pct%",
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

// ── Goal Management Bottom Sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalManagementSheet(
    goals: List<GoalWithProgress>,
    workoutTypes: List<WorkoutType>,
    onAddGoal: (GoalPeriod, Int, Long?, YearMonth, Boolean) -> Unit,
    onUpdateGoal: (Long, GoalPeriod, Int, Long?, YearMonth, Boolean) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onDismiss: () -> Unit,
    initialEditGoalId: Long? = null,
    initialBoundYearMonth: YearMonth = YearMonth.now()
) {
    var editingGoalId by remember { mutableStateOf(initialEditGoalId) }
    var selectedPeriod by remember { mutableStateOf(GoalPeriod.MONTHLY) }
    var selectedTypeId by remember { mutableStateOf<Long?>(null) }
    var targetCount by remember { mutableIntStateOf(3) }
    var targetText by remember { mutableStateOf("3") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var boundYearMonth by remember { mutableStateOf(initialBoundYearMonth) }
    var showOnHome by remember { mutableStateOf(true) }
    var showBoundPicker by remember { mutableStateOf(false) }

    LaunchedEffect(editingGoalId) {
        val eg = goals.find { it.goal.id == editingGoalId }
        if (eg != null) {
            selectedPeriod = eg.goal.period
            selectedTypeId = eg.goal.workoutTypeId
            targetCount = eg.goal.targetCount
            targetText = eg.goal.targetCount.toString()
            showOnHome = eg.goal.showOnDashboard
            boundYearMonth = YearMonth.of(
                eg.goal.boundYear.takeIf { it > 0 } ?: YearMonth.now().year,
                eg.goal.boundMonth ?: 1
            )
        } else {
            selectedPeriod = GoalPeriod.MONTHLY
            selectedTypeId = null
            targetCount = 3
            targetText = "3"
            boundYearMonth = initialBoundYearMonth
            showOnHome = true
        }
    }

    val nonRestTypes = workoutTypes.filter { !it.isRestDay }
    val allWorkoutsLabel = stringResource(R.string.goals_all_workouts)
    val selectedTypeName = nonRestTypes.find { it.id == selectedTypeId }?.name ?: allWorkoutsLabel
    val accent = selectedPeriod.accentColor()
    val boundDisplayLabel = when (selectedPeriod) {
        GoalPeriod.MONTHLY -> boundYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        GoalPeriod.YEARLY  -> boundYearMonth.year.toString()
    }

    val displayedGoals = remember(goals, boundYearMonth, selectedPeriod) {
        goals.filter { gwp ->
            when (gwp.goal.period) {
                GoalPeriod.MONTHLY ->
                    selectedPeriod == GoalPeriod.MONTHLY &&
                    gwp.goal.boundYear == boundYearMonth.year &&
                    gwp.goal.boundMonth == boundYearMonth.monthValue
                GoalPeriod.YEARLY ->
                    gwp.goal.boundYear == boundYearMonth.year
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Top bar — matches GoalsScreen header style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.goals_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Scrollable form content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    // Existing goals list
                    if (displayedGoals.isNotEmpty()) {
                        displayedGoals.forEach { gp ->
                            val goalAccent = gp.goal.period.accentColor()
                            val isEditing = editingGoalId == gp.goal.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isEditing) goalAccent.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .clickable { editingGoalId = gp.goal.id }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(goalAccent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gp.goal.period.letter(),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = goalAccent,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${stringResource(when (gp.goal.period) { GoalPeriod.MONTHLY -> R.string.goals_period_monthly; GoalPeriod.YEARLY -> R.string.goals_period_yearly })} · ${gp.goal.workoutType?.name ?: allWorkoutsLabel}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.goals_target_progress, gp.goal.targetCount, gp.current, gp.goal.targetCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (isEditing) editingGoalId = null
                                        onDeleteGoal(gp.goal.id)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.goals_delete_cd),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Form section header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (editingGoalId != null) stringResource(R.string.goals_edit_header) else stringResource(R.string.goals_new_header),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (editingGoalId != null) {
                            TextButton(onClick = { editingGoalId = null }) {
                                Text(stringResource(R.string.goals_new_btn), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Period type chips
                    Text(
                        text = stringResource(R.string.goals_period_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = {
                                    Text(
                                        text = when (period) {
                                            GoalPeriod.MONTHLY -> stringResource(R.string.goals_period_monthly)
                                            GoalPeriod.YEARLY  -> stringResource(R.string.goals_period_yearly)
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    // Date picker + Home toggle side by side
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.goals_for_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left half: date picker
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showBoundPicker = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = boundDisplayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = accent
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Right half: Home on/off toggle pill
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (showOnHome) accent.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (showOnHome) accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { showOnHome = !showOnHome }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = if (showOnHome) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.goals_home_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (showOnHome) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (showOnHome) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Workout type dropdown
                    Text(
                        text = stringResource(R.string.goals_workout_type_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTypeName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(allWorkoutsLabel) },
                                onClick = {
                                    selectedTypeId = null
                                    typeDropdownExpanded = false
                                }
                            )
                            nonRestTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        selectedTypeId = type.id
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Target count
                    Text(
                        text = stringResource(R.string.goals_target_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (targetCount > 1) {
                                    targetCount--
                                    targetText = targetCount.toString()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                targetText = digits
                                val num = digits.toIntOrNull()
                                if (num != null && num in 1..365) targetCount = num
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(80.dp)
                        )
                        IconButton(
                            onClick = {
                                if (targetCount < 365) {
                                    targetCount++
                                    targetText = targetCount.toString()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } // end scrollable Column

                // Save / Update button
                Button(
                    onClick = {
                        val eid = editingGoalId
                        if (eid != null) {
                            onUpdateGoal(eid, selectedPeriod, targetCount, selectedTypeId, boundYearMonth, showOnHome)
                            onDismiss()
                        } else {
                            onAddGoal(selectedPeriod, targetCount, selectedTypeId, boundYearMonth, showOnHome)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (editingGoalId != null) stringResource(R.string.goals_update_btn) else stringResource(R.string.goals_add_btn))
                }
            }
        }
    }

    if (showBoundPicker) {
        when (selectedPeriod) {
            GoalPeriod.MONTHLY -> MonthYearPickerDialog(
                currentMonth = boundYearMonth,
                onDismiss = { showBoundPicker = false },
                onConfirm = { boundYearMonth = it; showBoundPicker = false }
            )
            GoalPeriod.YEARLY -> YearPickerDialog(
                currentYear = boundYearMonth.year,
                onDismiss = { showBoundPicker = false },
                onConfirm = { boundYearMonth = YearMonth.of(it, 1); showBoundPicker = false }
            )
        }
    }
}
