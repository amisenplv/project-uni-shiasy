package org.unishiasy.vehiclelog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.unishiasy.vehiclelog.data.cloud.CloudBinding
import org.unishiasy.vehiclelog.data.cloud.CloudFailure
import org.unishiasy.vehiclelog.data.cloud.CloudIssue
import org.unishiasy.vehiclelog.data.cloud.CloudRepository
import org.unishiasy.vehiclelog.data.cloud.CloudRestoreSelection
import org.unishiasy.vehiclelog.data.cloud.CloudSlot
import org.unishiasy.vehiclelog.data.cloud.CloudStatus

internal data class CloudSettingsState(
    val status: CloudStatus? = null,
    val busy: Boolean = false,
    val sending: Boolean = false,
    val issue: CloudIssue? = null,
    val review: CloudSlot? = null,
    val restoreSelection: CloudRestoreSelection? = null,
    val restoring: Boolean = false,
)

internal class CloudSettingsViewModel(private val repository: CloudRepository) : ViewModel() {
    private val mutable = MutableStateFlow(CloudSettingsState())
    val state = mutable.asStateFlow()
    private var operation: Job? = null
    // Memory only. Separate from the persistent model and from saved instance state.
    private val mutableRecovery = MutableStateFlow<String?>(null)
    val recovery = mutableRecovery.asStateFlow()

    fun load() = action(keepRestoreSelection = true) {}
    fun authenticate(username: String, password: String, invite: String?) = action {
        mutableRecovery.value = repository.signIn(username, password, invite)
    }

    fun recoverAccount(username: String, password: String, code: String) = action {
        mutableRecovery.value = repository.recoverAccount(username, code, password)
    }

    fun prepareRestore() = action(recoveryAction = true) {
        val selected = repository.selectRestore()
        mutable.update { it.copy(restoreSelection = selected) }
    }

    fun confirmRestore() {
        val selected = mutable.value.restoreSelection ?: return
        action(restoring = true, recoveryAction = true) {
            val result = repository.restore(selected)
            mutable.update { it.copy(issue = result) }
        }
    }

    fun dismissRestore() { mutable.update { it.copy(restoreSelection = null) } }
    fun dismissRecovery() { mutableRecovery.value = null }
    fun signOut() = action {
        dismissRecovery()
        repository.signOut()
    }
    fun bind() = action { repository.bind() }
    fun upload() = action(sending = true) {
        val review = repository.startUpload()
        if (review == null) uploaded() else mutable.update { it.copy(review = review) }
    }
    fun retry() = action(sending = true) {
        repository.retry()
        uploaded()
    }
    fun review() = action {
        val slot = repository.review()
        mutable.update { it.copy(review = slot) }
    }
    fun confirmReplacement() {
        val reviewed = mutable.value.review ?: return
        action(sending = true) {
            repository.replaceAfterConfirmation(reviewed)
            uploaded()
        }
    }
    fun dismissReview() { mutable.update { it.copy(review = null) } }
    fun cancelUpload() { if (mutable.value.sending) operation?.cancel() }
    fun close() {
        if (!mutable.value.busy) {
            dismissRecovery()
            dismissReview()
            dismissRestore()
        }
    }

    private fun uploaded() { mutable.update { it.copy(issue = CloudIssue.Uploaded) } }

    private fun action(
        sending: Boolean = false,
        restoring: Boolean = false,
        recoveryAction: Boolean = false,
        keepRestoreSelection: Boolean = false,
        work: suspend () -> Unit,
    ) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, sending = sending, restoring = restoring, issue = null, review = null,
                restoreSelection = it.restoreSelection.takeIf { keepRestoreSelection }) }
        operation = viewModelScope.launch {
            try {
                work()
            } catch (error: CloudFailure) {
                val issue = if (recoveryAction && error.issue == CloudIssue.Unconfirmed) CloudIssue.RestoreInvalid else error.issue
                mutable.update { it.copy(issue = issue) }
            } catch (error: CancellationException) {
                if (sending) mutable.update { it.copy(issue = CloudIssue.Unconfirmed) }
                throw error
            } catch (_: Exception) {
                mutable.update { it.copy(issue = CloudIssue.Storage) }
            } finally {
                withContext(NonCancellable) {
                    try {
                        val status = repository.status()
                        mutable.update {
                            it.copy(
                                status = status,
                                issue = if (status.expired) CloudIssue.Expired else it.issue,
                                restoreSelection = it.restoreSelection.takeIf { status.binding == CloudBinding.Bound },
                            )
                        }
                    } catch (_: Exception) {
                        mutable.update { it.copy(status = null, issue = CloudIssue.Storage, restoreSelection = null) }
                    }
                    mutable.update { it.copy(busy = false, sending = false, restoring = false) }
                }
            }
        }
    }
}
