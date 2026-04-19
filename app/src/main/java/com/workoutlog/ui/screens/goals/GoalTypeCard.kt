package com.workoutlog.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private val MonthlyAccent   = Color(0xFF9C6ADE)
private val YearlyAccent    = Color(0xFFD4720A)
internal val HitColor        = Color(0xFF4A9B6F)
internal val MissColor       = Color(0xFFE05252)
internal val InProgressColor = Color(0xFFF59E0B)

@Composable
fun MonthlyGoalTypeCard(
    group: MonthlyGoalTypeGroup,
    onToggleDashboard: (goalId: Long, show: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSlot = group.months.firstOrNull { it.isActive && it.hasGoal }

    GoalTypeCardShell(
        typeName = group.workoutTypeName,
        accentColor = MonthlyAccent,
        periodBadge = "M",
        activeSlot = activeSlot?.let {
            ActiveSlotInfo(
                label = Month.of(it.month).getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${group.year}",
                achieved = it.achieved,
                target = it.target
            )
        },
        activeGoalId = group.activeGoalId,
        activeShowOnDashboard = group.activeShowOnDashboard,
        onToggleDashboard = onToggleDashboard,
        modifier = modifier
    ) {
        SlotRow(
            slots = group.months.map { slot ->
                SlotData(
                    label = Month.of(slot.month).getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                    hasGoal = slot.hasGoal,
                    isHit = slot.isHit,
                    isActive = slot.isActive
                )
            }
        )
    }
}

@Composable
fun YearlyGoalTypeCard(
    group: YearlyGoalTypeGroup,
    onToggleDashboard: (goalId: Long, show: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSlot = group.years.firstOrNull { it.isActive && it.hasGoal }

    GoalTypeCardShell(
        typeName = group.workoutTypeName,
        accentColor = YearlyAccent,
        periodBadge = "Y",
        activeSlot = activeSlot?.let {
            ActiveSlotInfo(
                label = it.year.toString(),
                achieved = it.achieved,
                target = it.target
            )
        },
        activeGoalId = group.activeGoalId,
        activeShowOnDashboard = group.activeShowOnDashboard,
        onToggleDashboard = onToggleDashboard,
        modifier = modifier
    ) {
        SlotRow(
            slots = group.years.map { slot ->
                SlotData(
                    label = "'${slot.year.toString().takeLast(2)}",
                    hasGoal = slot.hasGoal,
                    isHit = slot.isHit,
                    isActive = slot.isActive
                )
            }
        )
    }
}

private data class ActiveSlotInfo(val label: String, val achieved: Int, val target: Int)
private data class SlotData(val label: String, val hasGoal: Boolean, val isHit: Boolean, val isActive: Boolean)

@Composable
private fun GoalTypeCardShell(
    typeName: String,
    accentColor: Color,
    periodBadge: String,
    activeSlot: ActiveSlotInfo?,
    activeGoalId: Long?,
    activeShowOnDashboard: Boolean,
    onToggleDashboard: (goalId: Long, show: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isCollapsed = !isCollapsed }
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = periodBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = typeName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            // Per-goal dashboard toggle (only when there's an active goal)
            if (activeGoalId != null) {
                IconButton(
                    onClick = { onToggleDashboard(activeGoalId, !activeShowOnDashboard) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = if (activeShowOnDashboard) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            IconButton(
                onClick = { isCollapsed = !isCollapsed },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (!isCollapsed) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                // Active progress bar — dashboard style (Box-based, not LinearProgressIndicator)
                if (activeSlot != null) {
                    val progress = if (activeSlot.target > 0)
                        (activeSlot.achieved.toFloat() / activeSlot.target).coerceIn(0f, 1f)
                    else 0f
                    val pct = (progress * 100).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeSlot.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${activeSlot.achieved} / ${activeSlot.target}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
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
                                    .fillMaxWidth(fraction = progress)
                                    .background(accentColor)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$pct%",
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            fontSize = 10.sp,
                            lineHeight = 10.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                content()
            }
        }
    }
}

@Composable
private fun SlotRow(slots: List<SlotData>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        slots.forEach { slot ->
            SlotCell(
                label = slot.label,
                hasGoal = slot.hasGoal,
                isHit = slot.isHit,
                isActive = slot.isActive,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SlotStatusDot(background: Color, icon: ImageVector, iconTint: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(10.dp)
        )
    }
}

@Composable
private fun SlotCell(
    label: String,
    hasGoal: Boolean,
    isHit: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        when {
            !hasGoal -> Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                )
            }
            isActive -> SlotStatusDot(InProgressColor, Icons.Default.MoreHoriz, Color.Black.copy(alpha = 0.75f))
            isHit    -> SlotStatusDot(HitColor,        Icons.Default.Check,     Color.White)
            else     -> SlotStatusDot(MissColor.copy(alpha = 0.7f), Icons.Default.Close, Color.White)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (hasGoal) label else "",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = when {
                !hasGoal -> Color.Transparent
                isActive -> InProgressColor
                isHit    -> HitColor
                else     -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            },
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
