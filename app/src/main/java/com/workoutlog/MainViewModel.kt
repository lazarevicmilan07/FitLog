package com.workoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.billing.BillingManager
import com.workoutlog.data.datastore.SettingsDataStore
import com.workoutlog.data.datastore.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    init {
        viewModelScope.launch {
            billingManager.purchaseState.collect { state ->
                if (state is BillingManager.PurchaseState.Success) {
                    settingsDataStore.setPremium(true)
                }
            }
        }
    }

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val onboardingCompleted: StateFlow<Boolean?> = settingsDataStore.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPremium: StateFlow<Boolean> = settingsDataStore.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isStatsMonthly = MutableStateFlow(true)
    val isStatsMonthly: StateFlow<Boolean> = _isStatsMonthly.asStateFlow()

    fun setStatsMonthly(isMonthly: Boolean) {
        _isStatsMonthly.value = isMonthly
    }
}
