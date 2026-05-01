package com.workoutlog.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.ContextWrapper
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
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
import com.workoutlog.R
import com.workoutlog.data.preferences.LanguagePreferences
import com.workoutlog.data.preferences.supportedLanguages
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.data.datastore.ThemeMode
import com.workoutlog.notifications.BackupMonthlyOption
import com.workoutlog.notifications.BackupReminderFrequency
import com.workoutlog.notifications.BackupReminderSettings
import com.workoutlog.ui.components.MultiSelectMonthGrid
import com.workoutlog.ui.components.MultiSelectYearGrid
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }
    var showBackupReminderTimeDialog by remember { mutableStateOf(false) }
    var showBackupReminderDayOfWeekDialog by remember { mutableStateOf(false) }
    var showBackupReminderDayOfMonthDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguage = remember { LanguagePreferences.getLanguage(context) }

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setReminderEnabled(true)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.settings_notif_permission),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val backupNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setBackupReminderEnabled(true)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.settings_notif_permission),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Export / backup dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf("excel") } // "excel", "pdf", "backup"
    var pendingIsMonthly by remember { mutableStateOf(true) }
    var pendingSelectedMonths by remember { mutableStateOf<List<YearMonth>>(emptyList()) }
    var pendingSelectedYears by remember { mutableStateOf<List<Int>>(emptyList()) }

    // Restore confirmation dialog state
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // Launchers
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.backup(it, pendingIsMonthly, pendingSelectedMonths, pendingSelectedYears) }
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
        uri?.let { viewModel.exportToExcel(it, pendingIsMonthly, pendingSelectedMonths, pendingSelectedYears) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.exportToPdf(it, pendingIsMonthly, pendingSelectedMonths, pendingSelectedYears) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message -> {
                    val msg = if (event.arg != null) context.getString(event.resId, event.arg)
                               else context.getString(event.resId)
                    snackbarHostState.showSnackbar(msg)
                }
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
                    text = stringResource(R.string.settings_title),
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
                PremiumBanner(onClick = { onShowPremium() })
                Spacer(Modifier.height(20.dp))
            }

            // Appearance section
            SectionTitle(stringResource(R.string.settings_section_appearance))
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
                        text = stringResource(R.string.settings_theme),
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
                                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_auto)
                                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
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

            // Language section
            SectionTitle(stringResource(R.string.settings_section_language))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                val selectedLang = supportedLanguages.find { it.code == currentLanguage }
                SettingsActionRow(
                    icon = Icons.Default.Language,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_language),
                    subtitle = selectedLang?.nativeName ?: "English",
                    isLoading = false,
                    onClick = { showLanguageDialog = true }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Data section
            SectionTitle(stringResource(R.string.settings_section_data))
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
                        title = stringResource(R.string.settings_export_excel),
                        subtitle = stringResource(R.string.settings_export_excel_subtitle),
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
                        title = stringResource(R.string.settings_export_pdf),
                        subtitle = stringResource(R.string.settings_export_pdf_subtitle),
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
                        title = stringResource(R.string.settings_backup),
                        subtitle = if (isPremium) stringResource(R.string.settings_backup_subtitle) else stringResource(R.string.settings_premium_feature),
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
                        title = stringResource(R.string.settings_restore),
                        subtitle = if (isPremium) stringResource(R.string.settings_restore_subtitle) else stringResource(R.string.settings_premium_feature),
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
            SectionTitle(stringResource(R.string.settings_section_notifications))
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
                                text = stringResource(R.string.settings_daily_reminder),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.settings_daily_reminder_subtitle),
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
                                    text = stringResource(R.string.settings_reminder_time),
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Backup Reminder toggle (premium feature)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isPremium) {
                                    onShowPremium()
                                } else if (!backupReminderSettings.enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        backupNotificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    } else {
                                        viewModel.setBackupReminderEnabled(true)
                                    }
                                } else {
                                    viewModel.setBackupReminderEnabled(false)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBox(
                            icon = Icons.Default.Backup,
                            tint = if (isPremium) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_backup_reminder),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isPremium) stringResource(R.string.settings_backup_reminder_subtitle)
                                       else stringResource(R.string.settings_premium_feature),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isPremium) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Premium",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Switch(
                                checked = backupReminderSettings.enabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        backupNotificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    } else {
                                        viewModel.setBackupReminderEnabled(enabled)
                                    }
                                }
                            )
                        }
                    }

                    // Expanded backup reminder config (only when premium + enabled)
                    if (isPremium && backupReminderSettings.enabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        // Frequency row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsIconBox(
                                icon = Icons.Default.CalendarMonth,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.settings_frequency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(2.dp)
                            ) {
                                BackupReminderFrequency.entries.forEach { freq ->
                                    val isSelected = backupReminderSettings.frequency == freq
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.updateBackupReminderSettings(
                                                    backupReminderSettings.copy(frequency = freq)
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (freq) {
                                                BackupReminderFrequency.DAILY   -> stringResource(R.string.settings_freq_daily)
                                                BackupReminderFrequency.WEEKLY  -> stringResource(R.string.settings_freq_weekly)
                                                BackupReminderFrequency.MONTHLY -> stringResource(R.string.settings_freq_monthly)
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
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        // Time row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBackupReminderTimeDialog = true }
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
                                    text = stringResource(R.string.settings_reminder_time),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "%02d:%02d".format(backupReminderSettings.hour, backupReminderSettings.minute),
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
                        // Day of week row (weekly only)
                        if (backupReminderSettings.frequency == BackupReminderFrequency.WEEKLY) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 58.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showBackupReminderDayOfWeekDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SettingsIconBox(
                                    icon = Icons.Default.CalendarToday,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_day_of_week),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = DayOfWeek.of(backupReminderSettings.dayOfWeek)
                                            .getDisplayName(TextStyle.FULL, Locale.getDefault()),
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
                        // Day of month row (monthly only)
                        if (backupReminderSettings.frequency == BackupReminderFrequency.MONTHLY) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 58.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showBackupReminderDayOfMonthDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SettingsIconBox(
                                    icon = Icons.Default.CalendarMonth,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_day_of_month),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = backupReminderSettings.monthlyOption.label,
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
            }

            Spacer(Modifier.height(20.dp))

            // About section
            SectionTitle(stringResource(R.string.settings_section_about))
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
                            text = stringResource(R.string.app_name_full),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
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
        val dialogTitle = if (pendingExportFormat == "backup") stringResource(R.string.settings_backup_data_title) else stringResource(R.string.settings_export_report_title)
        ExportPeriodPickerDialog(
            title = dialogTitle,
            onDismiss = { showExportDialog = false },
            onConfirm = { isMonthly, months, years ->
                pendingIsMonthly = isMonthly
                pendingSelectedMonths = months
                pendingSelectedYears = years
                showExportDialog = false

                fun monthlyFilename(ext: String): String {
                    return if (months.size == 1) {
                        "WorkoutLog_${months[0].year}_${"%02d".format(months[0].monthValue)}.$ext"
                    } else {
                        val first = months.first(); val last = months.last()
                        "WorkoutLog_${first.year}_${"%02d".format(first.monthValue)}_to_${last.year}_${"%02d".format(last.monthValue)}.$ext"
                    }
                }
                fun yearlyFilename(ext: String): String {
                    return if (years.size == 1) "WorkoutLog_${years[0]}_Yearly.$ext"
                    else "WorkoutLog_${years.first()}_to_${years.last()}_Yearly.$ext"
                }

                when (pendingExportFormat) {
                    "excel" -> excelLauncher.launch(if (isMonthly) monthlyFilename("xlsx") else yearlyFilename("xlsx"))
                    "pdf"   -> pdfLauncher.launch(if (isMonthly) monthlyFilename("pdf") else yearlyFilename("pdf"))
                    "backup" -> {
                        val fname = if (isMonthly) monthlyFilename("json").replace("WorkoutLog_", "WorkoutLog_Backup_")
                                    else yearlyFilename("json").replace("WorkoutLog_", "WorkoutLog_Backup_").replace("_Yearly", "")
                        backupLauncher.launch(fname)
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
                    text = stringResource(R.string.app_name_full),
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
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.app_name_full),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\u00A9 ${java.time.Year.now().value} ${stringResource(R.string.app_name_full)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
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
                    Text(stringResource(R.string.settings_rate_app))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.btn_close))
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

    // Backup reminder time picker
    if (showBackupReminderTimeDialog) {
        ReminderTimePickerDialog(
            currentHour = backupReminderSettings.hour,
            currentMinute = backupReminderSettings.minute,
            onDismiss = { showBackupReminderTimeDialog = false },
            onTimeSelected = { hour, minute ->
                viewModel.updateBackupReminderSettings(
                    backupReminderSettings.copy(hour = hour, minute = minute)
                )
                showBackupReminderTimeDialog = false
            }
        )
    }

    // Backup reminder day-of-week picker
    if (showBackupReminderDayOfWeekDialog) {
        BackupReminderDayOfWeekDialog(
            selected = backupReminderSettings.dayOfWeek,
            onConfirm = { day ->
                viewModel.updateBackupReminderSettings(
                    backupReminderSettings.copy(dayOfWeek = day)
                )
                showBackupReminderDayOfWeekDialog = false
            },
            onDismiss = { showBackupReminderDayOfWeekDialog = false }
        )
    }

    // Backup reminder day-of-month picker
    if (showBackupReminderDayOfMonthDialog) {
        BackupReminderDayOfMonthDialog(
            selected = backupReminderSettings.monthlyOption,
            onConfirm = { option ->
                viewModel.updateBackupReminderSettings(
                    backupReminderSettings.copy(monthlyOption = option)
                )
                showBackupReminderDayOfMonthDialog = false
            },
            onDismiss = { showBackupReminderDayOfMonthDialog = false }
        )
    }

    // Language picker dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.lang_picker_title)) },
            text = {
                Column {
                    supportedLanguages.forEach { lang ->
                        val isSelected = lang.code == currentLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LanguagePreferences.setLanguage(context, lang.code)
                                    showLanguageDialog = false
                                    // On some OEM ROMs, LocalContext.current is a ContextWrapper
                                    // (e.g. ContextThemeWrapper) rather than the Activity itself,
                                    // so a direct cast fails and recreate() is never called.
                                    // Traverse the wrapper chain to find the real Activity.
                                    var ctx: android.content.Context = context
                                    while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
                                    (ctx as? Activity)?.recreate()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
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
            title = { Text(stringResource(R.string.settings_restore_title)) },
            text = {
                Text(stringResource(R.string.settings_restore_message))
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
                    Text(stringResource(R.string.settings_restore_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun ExportPeriodPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (isMonthly: Boolean, selectedMonths: List<YearMonth>, selectedYears: List<Int>) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var isMonthly by rememberSaveable { mutableStateOf(true) }

    var selectedMonthPairs by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var selectedYearSet by remember { mutableStateOf(setOf<Int>()) }
    var navYear by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }

    val baseYear = LocalDate.now().year
    val years = ((baseYear - 10)..(baseYear + 5)).toList()

    val stepTitle = when (step) {
        1    -> title
        else -> if (isMonthly) stringResource(R.string.export_select_month)
                else stringResource(R.string.export_select_year)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 1) {
                    IconButton(onClick = { step = 1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stepTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            when (step) {
                1 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodTypeOption(
                        icon = Icons.Default.CalendarToday,
                        title = stringResource(R.string.export_monthly_title),
                        description = stringResource(R.string.export_monthly_desc),
                        onClick = {
                            isMonthly = true
                            selectedMonthPairs = emptySet()
                            step = 2
                        }
                    )
                    PeriodTypeOption(
                        icon = Icons.Default.CalendarMonth,
                        title = stringResource(R.string.export_yearly_title),
                        description = stringResource(R.string.export_yearly_desc),
                        onClick = {
                            isMonthly = false
                            selectedYearSet = emptySet()
                            step = 2
                        }
                    )
                }
                else -> if (isMonthly) {
                    val selectedInNavYear = selectedMonthPairs
                        .filter { it.first == navYear }.map { it.second }.toSet()
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navYear-- }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous year")
                            }
                            Text(navYear.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { navYear++ }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next year")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        MultiSelectMonthGrid(
                            selectedMonths = selectedInNavYear,
                            onToggle = { month ->
                                val key = navYear to month
                                selectedMonthPairs = if (key in selectedMonthPairs)
                                    selectedMonthPairs - key else selectedMonthPairs + key
                            }
                        )
                        if (selectedMonthPairs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.export_n_selected, selectedMonthPairs.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                } else {
                    Column {
                        MultiSelectYearGrid(
                            years = years,
                            selectedYears = selectedYearSet,
                            onToggle = { yr ->
                                selectedYearSet = if (yr in selectedYearSet)
                                    selectedYearSet - yr else selectedYearSet + yr
                            }
                        )
                        if (selectedYearSet.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.export_n_selected, selectedYearSet.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                val canConfirm = if (isMonthly) selectedMonthPairs.isNotEmpty() else selectedYearSet.isNotEmpty()
                TextButton(enabled = canConfirm, onClick = {
                    if (isMonthly) {
                        val months = selectedMonthPairs
                            .sortedWith(compareBy({ it.first }, { it.second }))
                            .map { (y, m) -> YearMonth.of(y, m) }
                        onConfirm(true, months, emptyList())
                    } else {
                        onConfirm(false, emptyList(), selectedYearSet.sorted())
                    }
                }) { Text(stringResource(R.string.btn_ok)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
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
                    text = stringResource(R.string.settings_reminder_time_title),
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
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                        Text(stringResource(R.string.btn_ok))
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
                    text = stringResource(R.string.settings_upgrade_premium),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_upgrade_subtitle),
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

@Composable
private fun BackupReminderDayOfWeekDialog(
    selected: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableIntStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_day_of_week)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..7).forEach { day ->
                    val isSelected = day == current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { current = day },
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) { Text(stringResource(R.string.btn_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@Composable
private fun BackupReminderDayOfMonthDialog(
    selected: BackupMonthlyOption,
    onConfirm: (BackupMonthlyOption) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_day_of_month)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BackupMonthlyOption.entries.forEach { option ->
                    val isSelected = option == current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { current = option },
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) { Text(stringResource(R.string.btn_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
