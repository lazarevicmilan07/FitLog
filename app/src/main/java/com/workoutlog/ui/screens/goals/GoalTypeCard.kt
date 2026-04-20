package com.workoutlog.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        // Left accent strip
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentColor.copy(alpha = 0.7f))
        )

        Column(modifier = Modifier.weight(1f)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCollapsed = !isCollapsed }
                    .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = periodBadge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = typeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Compact X/Y chip — visible even when collapsed
                if (activeSlot != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${activeSlot.achieved}/${activeSlot.target}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            lineHeight = 11.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                // Per-goal dashboard toggle pill — always shown, clickable when any goal exists
                val dashActive = activeGoalId != null && activeShowOnDashboard
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (dashActive)
                                Modifier.background(accentColor.copy(alpha = 0.15f))
                            else
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        )
                        .then(
                            if (activeGoalId != null)
                                Modifier.clickable { onToggleDashboard(activeGoalId, !dashActive) }
                            else
                                Modifier
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = if (dashActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Home",
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        fontWeight = if (dashActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (dashActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Spacer(Modifier.width(4.dp))
                // Collapse indicator (no separate click — Row handles it)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
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
                Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp)) {
                    // Active progress bar
                    if (activeSlot != null) {
                        val progress = if (activeSlot.target > 0)
                            (activeSlot.achieved.toFloat() / activeSlot.target).coerceIn(0f, 1f)
                        else 0f
                        val pct = (progress * 100).toInt()

                        Text(
                            text = activeSlot.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
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
            .size(18.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(11.dp)
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
            !hasGoal -> Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
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
