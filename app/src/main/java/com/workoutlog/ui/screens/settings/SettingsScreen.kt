package com.workoutlog.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.data.datastore.ThemeMode
import com.workoutlog.ui.components.MonthGrid
import com.workoutlog.ui.components.YearGrid
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAboutDialog by remember { mutableStateOf(false) }

    // Export / backup dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf("excel") } // "excel", "pdf", "backup"
    var pendingIsMonthly by remember { mutableStateOf(true) }
    var pendingExportYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var pendingExportMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }

    // Restore confirmation dialog state
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // Launchers
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.backup(it, pendingIsMonthly, pendingExportYear, pendingExportMonth) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restore(it) }
    }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        uri?.let { viewModel.exportToExcel(it, pendingIsMonthly, pendingExportYear, pendingExportMonth) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.exportToPdf(it, pendingIsMonthly, pendingExportYear, pendingExportMonth) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
            // Appearance section
            SectionTitle("Appearance")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconBox(
                        icon = Icons.Default.DarkMode,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    // Compact segmented pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = state.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.SYSTEM -> "Auto"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Data section
            SectionTitle("Data")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.TableChart,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Export to Excel",
                        subtitle = "Export workout report as spreadsheet",
                        isLoading = state.isExportingExcel,
                        onClick = {
                            pendingExportFormat = "excel"
                            showExportDialog = true
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 58.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Description,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Export to PDF",
                        subtitle = "Export workout report as PDF document",
                        isLoading = state.isExportingPdf,
                        onClick = {
                            pendingExportFormat = "pdf"
                            showExportDialog = true
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 58.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Backup,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Backup data",
                        subtitle = "Export all data to JSON file",
                        isLoading = state.isBackingUp,
                        onClick = {
                            pendingExportFormat = "backup"
                            showExportDialog = true
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 58.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Restore,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Restore data",
                        subtitle = "Import data from JSON backup",
                        isLoading = state.isRestoring,
                        onClick = { showRestoreConfirmDialog = true }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // About section
            SectionTitle("About")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconBox(
                        icon = Icons.Default.Info,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Workout Log",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            } // end inner scrollable Column
        }
    }

    // Export / backup period picker dialog
    if (showExportDialog) {
        val dialogTitle = if (pendingExportFormat == "backup") "Backup Data" else "Export Report"
        ExportPeriodPickerDialog(
            title = dialogTitle,
            onDismiss = { showExportDialog = false },
            onConfirm = { isMonthly, year, month ->
                pendingIsMonthly = isMonthly
                pendingExportYear = year
                pendingExportMonth = month
                showExportDialog = false
                when (pendingExportFormat) {
                    "excel" -> {
                        val filename = if (isMonthly)
                            "WorkoutLog_${year}_${String.format("%02d", month)}.xlsx"
                        else
                            "WorkoutLog_${year}_Yearly.xlsx"
                        excelLauncher.launch(filename)
                    }
                    "pdf" -> {
                        val filename = if (isMonthly)
                            "WorkoutLog_${year}_${String.format("%02d", month)}.pdf"
                        else
                            "WorkoutLog_${year}_Yearly.pdf"
                        pdfLauncher.launch(filename)
                    }
                    "backup" -> {
                        val filename = if (isMonthly)
                            "WorkoutLog_Backup_${year}_${String.format("%02d", month)}.json"
                        else
                            "WorkoutLog_Backup_${year}.json"
                        backupLauncher.launch(filename)
                    }
                }
            }
        )
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Workout Log",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\u00A9 ${java.time.Year.now().value} Workout Log",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://lazarevicmilan07.github.io/workout-log/privacy-policy.html")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("market://details?id=${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: android.content.ActivityNotFoundException) {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Rate App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Restore confirmation dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Restore Data") },
            text = {
                Text(
                    "This will permanently delete all your current workout data and replace it with the data from the backup file.\n\nThis action cannot be undone. Are you sure you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        restoreLauncher.launch(arrayOf("application/json"))
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExportPeriodPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (isMonthly: Boolean, year: Int, month: Int) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var isMonthly by rememberSaveable { mutableStateOf(true) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }

    val baseYear = LocalDate.now().year
    val years = ((baseYear - 10)..(baseYear + 5)).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step == 2) {
                    IconButton(onClick = { step = 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (step == 1) title
                           else if (isMonthly) "Select Month"
                           else "Select Year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            when (step) {
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PeriodTypeOption(
                            icon = Icons.Default.CalendarToday,
                            title = "Monthly",
                            description = "Select data for a specific month",
                            onClick = { isMonthly = true; step = 2 }
                        )
                        PeriodTypeOption(
                            icon = Icons.Default.CalendarMonth,
                            title = "Yearly",
                            description = "Select data for a full year",
                            onClick = { isMonthly = false; step = 2 }
                        )
                    }
                }
                else -> {
                    if (isMonthly) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { selectedYear-- }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous year")
                                }
                                Text(
                                    text = selectedYear.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = { selectedYear++ }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next year")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            MonthGrid(
                                selectedMonth = selectedMonth,
                                onMonthSelected = { selectedMonth = it }
                            )
                        }
                    } else {
                        YearGrid(
                            years = years,
                            selectedYear = selectedYear,
                            onYearSelected = { selectedYear = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                TextButton(onClick = {
                    if (isMonthly) onConfirm(true, selectedYear, selectedMonth)
                    else onConfirm(false, selectedYear, 0)
                }) { Text("OK") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PeriodTypeOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsIconBox(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon = icon, tint = iconTint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
