package org.unishiasy.vehiclelog.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.data.backup.BackupOutcome
import org.unishiasy.vehiclelog.data.backup.BackupRepository

internal enum class BackupAction { Export, Restore }

internal data class BackupSettingsState(
    val open: Boolean = false,
    val action: BackupAction? = null,
    val working: Boolean = false,
    val outcome: BackupOutcome? = null,
)

internal class BackupSettingsViewModel(
    private val repository: BackupRepository,
    private val resolver: ContentResolver,
    private val savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoreInitialState(savedStateHandle))
    val state = mutableState.asStateFlow()

    fun open() {
        setState(BackupSettingsState(open = true))
    }

    fun back() {
        if (state.value.action == null) setState(BackupSettingsState())
    }

    fun startPicker(action: BackupAction): Boolean {
        if (!state.value.open || state.value.action != null) return false
        setState(BackupSettingsState(open = true, action = action))
        return true
    }

    fun pickerUnavailable(action: BackupAction) {
        if (state.value.action == action && !state.value.working) finish(BackupOutcome.FileUnavailable)
    }

    fun selected(action: BackupAction, uri: Uri?) {
        if (state.value.action != action || state.value.working) return
        if (uri == null) {
            finish(BackupOutcome.Canceled)
            return
        }
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            finish(BackupOutcome.FileUnavailable)
            return
        }
        setState(state.value.copy(working = true))
        viewModelScope.launch {
            val outcome = when (action) {
                BackupAction.Export -> repository.export { resolver.openOutputStream(uri, "wt") }
                BackupAction.Restore -> repository.restore { resolver.openInputStream(uri) }
            }
            finish(outcome)
        }
    }

    private fun finish(outcome: BackupOutcome) {
        setState(BackupSettingsState(open = true, outcome = outcome))
    }

    private fun setState(next: BackupSettingsState) {
        mutableState.value = next
        savedStateHandle?.let { handle ->
            handle[KEY_OPEN] = next.open
            handle[KEY_ACTION] = next.action?.name
        }
    }

    companion object {
        private const val KEY_OPEN = "backup_vm_open"
        private const val KEY_ACTION = "backup_vm_action"

        private fun restoreInitialState(handle: SavedStateHandle?): BackupSettingsState {
            if (handle == null) return BackupSettingsState()
            val open = handle.get<Boolean>(KEY_OPEN) == true
            val action = handle.get<String>(KEY_ACTION)?.let { name ->
                BackupAction.entries.firstOrNull { it.name == name }
            }
            return BackupSettingsState(open = open, action = action)
        }
    }
}
