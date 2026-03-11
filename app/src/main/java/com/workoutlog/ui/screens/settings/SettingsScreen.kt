package com.workoutlog.ui.screens.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.workoutlog.BuildConfig
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    onShowPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setReminderEnabled(true)
        } else {
            Toast.makeText(
                context,
                "Notification permission required for reminders",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
                is SettingsEvent.ShowPremiumRequired -> onShowPremium()
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
            // Premium banner
            if (!isPremium) {
                PremiumBanner(onClick = {
                    viewModel.setPremium(true)
                    Toast.makeText(context, "Premium activated! All features unlocked.", Toast.LENGTH_SHORT).show()
                })
                Spacer(Modifier.height(20.dp))
            }

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
                        subtitle = if (isPremium) "Export all data to JSON file" else "Premium feature",
                        isLoading = state.isBackingUp,
                        isPremiumLocked = !isPremium,
                        onClick = {
                            if (isPremium) {
                                pendingExportFormat = "backup"
                                showExportDialog = true
                            } else {
                                onShowPremium()
                            }
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
                        subtitle = if (isPremium) "Import data from JSON backup" else "Premium feature",
                        isLoading = state.isRestoring,
                        isPremiumLocked = !isPremium,
                        onClick = {
                            if (isPremium) showRestoreConfirmDialog = true
                            else onShowPremium()
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Notifications section
            SectionTitle("Notifications")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    // Enable / disable toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!reminderEnabled) {
                                    // Request POST_NOTIFICATIONS on Android 13+
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    } else {
                                        viewModel.setReminderEnabled(true)
                                    }
                                } else {
                                    viewModel.setReminderEnabled(false)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBox(
                            icon = Icons.Default.NotificationsActive,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reminder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Remind me to log my workout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    viewModel.setReminderEnabled(enabled)
                                }
                            }
                        )
                    }

                    // Reminder time row (only visible when reminders are enabled)
                    if (reminderEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showReminderTimeDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.AccessTime,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reminder Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = reminderTime.formatted(),
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
                            text = "Version ${BuildConfig.VERSION_NAME}",
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
                        text = "Version ${BuildConfig.VERSION_NAME}",
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

    // Reminder time picker dialog
    if (showReminderTimeDialog) {
        ReminderTimePickerDialog(
            currentHour = reminderTime.hour,
            currentMinute = reminderTime.minute,
            onDismiss = { showReminderTimeDialog = false },
            onTimeSelected = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showReminderTimeDialog = false
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
    onClick: () -> Unit,
    isPremiumLocked: Boolean = false
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
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            isPremiumLocked -> Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Premium",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            else -> Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Scroll-wheel time picker dialog (Samsung alarm clock style, 24-hour format). */
@Composable
private fun ReminderTimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by rememberSaveable { mutableIntStateOf(currentHour) }
    var selectedMinute by rememberSaveable { mutableIntStateOf(currentMinute) }
    var requestMinutesEdit by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Reminder Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeScrollColumn(
                        value = selectedHour,
                        range = 0..23,
                        onValueChange = { selectedHour = it },
                        onDone = { requestMinutesEdit = true },
                        imeAction = ImeAction.Next
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    TimeScrollColumn(
                        value = selectedMinute,
                        range = 0..59,
                        onValueChange = { selectedMinute = it },
                        requestEdit = requestMinutesEdit,
                        onEditStarted = { requestMinutesEdit = false }
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

/**
 * A vertical scroll-wheel drum for picking a single numeric value.
 *
 * - Scroll to change value; snaps to the nearest item on fling release.
 * - Tap the highlighted center item to switch to keyboard input mode.
 * - Top/bottom items fade out via a gradient overlay to reinforce the drum feel.
 */
@Composable
private fun TimeScrollColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onDone: (() -> Unit)? = null,
    requestEdit: Boolean = false,
    onEditStarted: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Done
) {
    val count = range.count()
    // Large multiplier gives the illusion of infinite scrolling
    val multiplier = 200
    val totalItems = count * multiplier
    val itemHeightDp = 52.dp

    val listState = rememberLazyListState(
        // Place `value` at the centre (index + 1 from first visible)
        initialFirstVisibleItemIndex = (multiplier / 2) * count + (value - range.first) - 1
    )
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Enter edit mode when triggered by the sibling column (e.g. hours → minutes)
    LaunchedEffect(requestEdit) {
        if (requestEdit) {
            editText = ""
            isEditing = true
            onEditStarted()
        }
    }

    // Centre item tracking: once scroll offset exceeds half an item height, the
    // item visually at center has already moved to firstVisibleItemIndex + 2.
    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }
    val selectedAbsoluteIndex by remember(itemHeightPx) {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > itemHeightPx / 2f) listState.firstVisibleItemIndex + 2
            else listState.firstVisibleItemIndex + 1
        }
    }

    // Keep parent state in sync with the visually selected item.
    // Using selectedAbsoluteIndex (derived from scroll position) as the key
    // means the value updates immediately as the user scrolls — no async delay
    // between the snap settling and Save being pressed.
    LaunchedEffect(selectedAbsoluteIndex) {
        onValueChange(range.first + (selectedAbsoluteIndex % count))
    }

    // Back press while editing exits edit mode instead of closing the dialog
    BackHandler(enabled = isEditing) {
        isEditing = false
        editText = ""
    }

    // "Hide keyboard" arrow (down button in nav bar) dismisses the keyboard without
    // triggering back. Detect this by watching IME visibility: when the keyboard
    // disappears while still in edit mode, exit edit mode so scrolling works again.
    // A short delay is used so that a transient hide caused by focus transferring
    // from hours to minutes does not incorrectly kill the minutes edit mode.
    @OptIn(ExperimentalLayoutApi::class)
    val imeVisible = WindowInsets.isImeVisible
    val imeVisibleState = rememberUpdatedState(imeVisible)
    LaunchedEffect(imeVisible) {
        if (isEditing && !imeVisible) {
            delay(200)
            if (!imeVisibleState.value && isEditing) {
                isEditing = false
                editText = ""
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.width(80.dp).height(itemHeightDp * 3)) {
        if (isEditing) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            BasicTextField(
                value = editText,
                onValueChange = { text ->
                    if (text.length <= 2 && text.all { it.isDigit() }) {
                        // Clamp to range.last if a 2-digit entry exceeds the maximum
                        // (e.g. "99" → "59" for minutes, "29" → "23" for hours)
                        val effective = if (text.length == 2) {
                            val n = text.toInt()
                            if (n > range.last) "%02d".format(range.last) else text
                        } else text

                        editText = effective
                        // Live-update parent so dialog OK always reflects what was typed
                        effective.toIntOrNull()?.let { num -> if (num in range) onValueChange(num) }
                        // Auto-advance when a second digit is entered, or when the
                        // single digit typed can't possibly be a valid tens digit
                        // (e.g. hours: digit ≥ 3 → no 2-digit hour starts with 3–9).
                        if (onDone != null) {
                            val shouldAdvance = effective.length == 2 ||
                                (effective.length == 1 && (effective.toIntOrNull() ?: 0) * 10 > range.last)
                            if (shouldAdvance) {
                                // Scroll the drum to the entered value before hiding
                                // the text field, so the wheel shows the correct position
                                effective.toIntOrNull()?.let { num ->
                                    if (num in range) {
                                        val targetFirst = (multiplier / 2) * count + (num - range.first) - 1
                                        scope.launch { listState.scrollToItem(targetFirst) }
                                    }
                                }
                                isEditing = false
                                editText = ""
                                onDone.invoke()
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val num = editText.toIntOrNull()
                        if (num != null && num in range) {
                            onValueChange(num)
                            val targetFirst = (multiplier / 2) * count + (num - range.first) - 1
                            scope.launch { listState.scrollToItem(targetFirst) }
                        }
                        isEditing = false
                        editText = ""
                        onDone?.invoke()
                    },
                    onNext = {
                        val num = editText.toIntOrNull()
                        if (num != null && num in range) {
                            onValueChange(num)
                            val targetFirst = (multiplier / 2) * count + (num - range.first) - 1
                            scope.launch { listState.scrollToItem(targetFirst) }
                        }
                        isEditing = false
                        editText = ""
                        onDone?.invoke()
                    }
                ),
                modifier = Modifier.align(Alignment.Center).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    color = primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .size(80.dp, itemHeightDp)
                            .background(primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editText.isEmpty()) {
                            Text(
                                text = "%02d".format(value),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = primary.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        // Fade top third
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(bgColor, Color.Transparent),
                                startY = 0f, endY = size.height / 3f
                            )
                        )
                        // Fade bottom third
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, bgColor),
                                startY = size.height * 2f / 3f, endY = size.height
                            )
                        )
                    }
            ) {
                items(totalItems) { absoluteIndex ->
                    val itemValue = range.first + (absoluteIndex % count)
                    val isSelected = absoluteIndex == selectedAbsoluteIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .then(
                                if (isSelected) Modifier.background(
                                    primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .clickable {
                                if (isSelected) {
                                    editText = ""
                                    isEditing = true
                                } else {
                                    scope.launch { listState.animateScrollToItem(absoluteIndex - 1) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(itemValue),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primary else onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Unlock all features with a one-time purchase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
