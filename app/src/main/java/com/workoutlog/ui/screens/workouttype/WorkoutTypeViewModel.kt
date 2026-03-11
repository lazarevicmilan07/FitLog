package com.workoutlog.ui.screens.workouttype

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutTypesUiState(
    val isLoading: Boolean = true,
    val types: List<WorkoutType> = emptyList()
)

@HiltViewModel
class WorkoutTypesViewModel @Inject constructor(
    private val repository: WorkoutTypeRepository
) : ViewModel() {

    val uiState: StateFlow<WorkoutTypesUiState> = repository.getAllFlow()
        .map { types ->
            WorkoutTypesUiState(
                isLoading = false,
                types = types.map { it.toDomain() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutTypesUiState())

    fun deleteType(type: WorkoutType) {
        viewModelScope.launch {
            repository.deleteById(type.id)
        }
    }
}

// Separate ViewModel for Add/Edit
data class AddEditTypeUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val typeId: Long = 0,
    val name: String = "",
    val selectedColor: Color = Color.Gray,
    val icon: String = "fitness_center",
    val isRestDay: Boolean = false,
    val isSaving: Boolean = false
)

sealed class AddEditTypeEvent {
    data object Saved : AddEditTypeEvent()
    data object Deleted : AddEditTypeEvent()
    data class Error(val message: String) : AddEditTypeEvent()
    /** Emitted when toggling rest day ON would displace an existing rest-day type. */
    data class ConfirmRestDayConflict(val existingTypeName: String) : AddEditTypeEvent()
}

@HiltViewModel
class AddEditWorkoutTypeViewModel @Inject constructor(
    private val repository: WorkoutTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTypeUiState())
    val uiState: StateFlow<AddEditTypeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditTypeEvent>()
    val events = _events.asSharedFlow()

    fun setup(typeId: Long) {
        _uiState.value = AddEditTypeUiState(isLoading = true)
        viewModelScope.launch {
            if (typeId > 0) {
                val entity = repository.getById(typeId)
                if (entity != null) {
                    val type = entity.toDomain()
                    _uiState.value = AddEditTypeUiState(
                        isLoading = false,
                        isEditing = true,
                        typeId = type.id,
                        name = type.name,
                        selectedColor = type.color,
                        icon = type.icon ?: "fitness_center",
                        isRestDay = type.isRestDay
                    )
                    return@launch
                }
            }
            _uiState.value = AddEditTypeUiState(isLoading = false)
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onColorSelected(color: Color) {
        _uiState.value = _uiState.value.copy(selectedColor = color)
    }

    fun onIconSelected(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun onRestDayChanged(isRestDay: Boolean) {
        if (!isRestDay) {
            _uiState.value = _uiState.value.copy(isRestDay = false)
            return
        }
        // Turning ON: check if another type already holds the rest-day flag
        viewModelScope.launch {
            val existing = repository.getRestDayType()
            val currentId = _uiState.value.typeId
            if (existing != null && existing.id != currentId) {
                // Another type owns the flag — ask the user to confirm the switch
                _events.emit(AddEditTypeEvent.ConfirmRestDayConflict(existing.name))
            } else {
                _uiState.value = _uiState.value.copy(isRestDay = true)
            }
        }
    }

    /** Called after the user confirms displacing the existing rest-day type. */
    fun confirmRestDayOverride() {
        _uiState.value = _uiState.value.copy(isRestDay = true)
    }

    fun delete() {
        val state = _uiState.value
        if (!state.isEditing) return
        viewModelScope.launch {
            repository.deleteById(state.typeId)
            _events.emit(AddEditTypeEvent.Deleted)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch { _events.emit(AddEditTypeEvent.Error("Name cannot be empty")) }
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val type = WorkoutType(
                id = if (state.isEditing) state.typeId else 0,
                name = state.name.trim(),
                color = state.selectedColor,
                icon = state.icon,
                isRestDay = state.isRestDay
            )

            if (state.isEditing) {
                repository.update(type.toEntity())
                // If this type is now the rest day, clear the flag from all others
                if (type.isRestDay) repository.clearRestDayFlagExcept(type.id)
            } else {
                val newId = repository.insert(type.toEntity())
                // If the new type is the rest day, clear the flag from all others
                if (type.isRestDay) repository.clearRestDayFlagExcept(newId)
            }

            _events.emit(AddEditTypeEvent.Saved)
        }
    }
}
