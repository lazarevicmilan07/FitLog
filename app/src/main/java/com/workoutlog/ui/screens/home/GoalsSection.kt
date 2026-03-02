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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.workoutlog.domain.model.GoalPeriod
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.label

private fun GoalPeriod.accentColor(): Color = when (this) {
    GoalPeriod.WEEKLY  -> Color(0xFF5B8DEE)  // blue
    GoalPeriod.MONTHLY -> Color(0xFF9C6ADE)  // purple
    GoalPeriod.YEARLY  -> Color(0xFFE8A838)  // amber
}

private fun GoalPeriod.shortLabel(): String = when (this) {
    GoalPeriod.WEEKLY  -> "Weekly"
    GoalPeriod.MONTHLY -> "Monthly"
    GoalPeriod.YEARLY  -> "Yearly"
}

private fun GoalPeriod.letter(): String = when (this) {
    GoalPeriod.WEEKLY  -> "W"
    GoalPeriod.MONTHLY -> "M"
    GoalPeriod.YEARLY  -> "Y"
}


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
                text = "Goals",
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
                    contentDescription = "Manage goals",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (goals.isEmpty()) {
            Text(
                text = "Tap + to set a goal",
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
    val typeName = goal.workoutType?.name ?: "All Workouts"
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
                text = "${goal.period.shortLabel()} · $typeName",
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
            if (isComplete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Goal completed",
                    tint = Color(0xFF4A9B6F),
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
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
    onAddGoal: (GoalPeriod, Int, Long?) -> Unit,
    onUpdateGoal: (Long, GoalPeriod, Int, Long?) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onDismiss: () -> Unit,
    initialEditGoalId: Long? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var editingGoalId by remember { mutableStateOf(initialEditGoalId) }
    var selectedPeriod by remember { mutableStateOf(GoalPeriod.WEEKLY) }
    var selectedTypeId by remember { mutableStateOf<Long?>(null) }
    var targetCount by remember { mutableIntStateOf(3) }
    var targetText by remember { mutableStateOf("3") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    // Pre-fill or reset form whenever editingGoalId changes
    LaunchedEffect(editingGoalId) {
        val eg = goals.find { it.goal.id == editingGoalId }
        if (eg != null) {
            selectedPeriod = eg.goal.period
            selectedTypeId = eg.goal.workoutTypeId
            targetCount = eg.goal.targetCount
            targetText = eg.goal.targetCount.toString()
        } else {
            selectedPeriod = GoalPeriod.WEEKLY
            selectedTypeId = null
            targetCount = 3
            targetText = "3"
        }
    }

    val nonRestTypes = workoutTypes.filter { !it.isRestDay }
    val selectedTypeName = nonRestTypes.find { it.id == selectedTypeId }?.name ?: "All Workouts"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Goals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Existing goals
            if (goals.isNotEmpty()) {
                goals.forEach { gp ->
                    val accent = gp.goal.period.accentColor()
                    val isEditing = editingGoalId == gp.goal.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isEditing) accent.copy(alpha = 0.08f)
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
                                .background(accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = gp.goal.period.letter(),
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${gp.goal.period.label()} · ${gp.goal.workoutType?.name ?: "All Workouts"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Target ${gp.goal.targetCount}  ·  ${gp.current}/${gp.goal.targetCount} this period",
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
                                contentDescription = "Delete goal",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Add / Edit goal form header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingGoalId != null) "Edit Goal" else "New Goal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (editingGoalId != null) {
                    TextButton(onClick = { editingGoalId = null }) {
                        Text("New Goal", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Text(
                text = "Period",
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
                                    GoalPeriod.WEEKLY  -> "Weekly"
                                    GoalPeriod.MONTHLY -> "Monthly"
                                    GoalPeriod.YEARLY  -> "Yearly"
                                },
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Workout Type",
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
                        text = { Text("All Workouts") },
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

            Text(
                text = "Target",
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
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

        } // end scrollable Column

        Button(
            onClick = {
                val eid = editingGoalId
                if (eid != null) {
                    onUpdateGoal(eid, selectedPeriod, targetCount, selectedTypeId)
                    onDismiss()
                } else {
                    onAddGoal(selectedPeriod, targetCount, selectedTypeId)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 12.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (editingGoalId != null) "Update Goal" else "Add Goal")
        }
        } // end outer Column
    }
}
