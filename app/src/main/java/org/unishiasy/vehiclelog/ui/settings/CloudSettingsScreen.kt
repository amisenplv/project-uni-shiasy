package org.unishiasy.vehiclelog.ui.settings

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.ui.components.DismissibleHeading
import org.unishiasy.vehiclelog.ui.components.LedgerTopBar
import org.unishiasy.vehiclelog.data.cloud.CloudBinding
import org.unishiasy.vehiclelog.data.cloud.CloudIssue
import org.unishiasy.vehiclelog.data.cloud.CloudRetry
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.withPersianDigits

@Composable
internal fun CloudSettingsScreen(viewModel: CloudSettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recovery by viewModel.recovery.collectAsStateWithLifecycle()
    val status = state.status
    var confirmBinding by remember { mutableStateOf(false) }
    val window = LocalActivity.current?.window

    DisposableEffect(window) {
        val secure = WindowManager.LayoutParams.FLAG_SECURE
        val wasSecure = window?.attributes?.flags?.and(secure) == secure
        window?.addFlags(secure)
        onDispose { if (!wasSecure) window?.clearFlags(secure) }
    }
    LaunchedEffect(viewModel) { viewModel.load() }
    LaunchedEffect(status?.expires) {
        status?.expires?.let { expires ->
            delay(Duration.between(Instant.now(), expires).toMillis().coerceAtLeast(1))
            viewModel.load() // A local expiry check. It cannot contact the server.
        }
    }
    val back = {
        if (!state.busy) {
            viewModel.close()
            onBack()
        }
    }
    BackHandler(onBack = back)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.cloud_title),
                onBack = back,
                backEnabled = !state.busy,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues).imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp).testTag("cloudScreen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            Text(stringResource(R.string.cloud_notice))
            HorizontalDivider()
            if (status != null) {
                val username = status.username
                if (username == null) {
                    CloudCredentials(!state.busy, viewModel::authenticate, viewModel::recoverAccount)
                } else {
                    Text(stringResource(R.string.cloud_account, "\u2066$username\u2069"),
                        style = MaterialTheme.typography.titleMedium)
                    when (status.binding) {
                        CloudBinding.Unbound, CloudBinding.OtherAccount -> {
                            if (status.binding == CloudBinding.OtherAccount) Text(stringResource(R.string.cloud_other_account))
                            OutlinedButton(
                                { confirmBinding = true },
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cloudBind"),
                                enabled = !state.busy,
                            ) { Text(stringResource(R.string.cloud_bind)) }
                        }
                        CloudBinding.Bound -> {
                            val ack = status.ack
                            if (ack == null) {
                                Text(stringResource(R.string.cloud_no_ack))
                            } else {
                                Text(stringResource(R.string.cloud_last_ack, cloudDate(ack.time)), Modifier.testTag("cloudAck"))
                                Text(stringResource(R.string.cloud_revision))
                                Text(ack.revision, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
                            }
                            if (status.retry != CloudRetry.None) Text(stringResource(R.string.cloud_pending))
                            Button(
                                onClick = when (status.retry) {
                                    CloudRetry.Pending -> viewModel::retry
                                    CloudRetry.Review -> viewModel::review
                                    CloudRetry.None -> viewModel::upload
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cloudUpload"),
                                enabled = !state.busy,
                            ) {
                                Text(stringResource(when (status.retry) {
                                    CloudRetry.Pending -> R.string.cloud_retry
                                    CloudRetry.Review -> R.string.cloud_review
                                    CloudRetry.None -> R.string.cloud_upload
                                }))
                            }
                            if (status.retry == CloudRetry.Pending) {
                                TextButton(
                                    viewModel::review,
                                    Modifier.heightIn(min = 48.dp),
                                    enabled = !state.busy,
                                ) { Text(stringResource(R.string.cloud_review)) }
                            }
                        }
                    }
                    if (status.binding == CloudBinding.Bound) {
                        HorizontalDivider()
                        Text(stringResource(R.string.cloud_restore_title), Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.cloud_restore_explained))
                        OutlinedButton(
                            viewModel::prepareRestore,
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cloudRestoreReview"),
                            enabled = !state.busy,
                        ) { Text(stringResource(R.string.cloud_restore_review)) }
                    }
                    TextButton(viewModel::signOut, Modifier.heightIn(min = 48.dp).testTag("cloudSignOut"),
                        enabled = !state.busy) {
                        Text(stringResource(R.string.cloud_sign_out))
                    }
                }
            }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(when {
                    state.sending -> R.string.cloud_sending
                    state.restoring -> R.string.cloud_restoring
                    else -> R.string.cloud_loading
                }),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (state.sending) {
                    TextButton(viewModel::cancelUpload, Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cloud_cancel_upload))
                    }
                }
            }
            state.issue?.let {
                Text(stringResource(issueLabel(it)), Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("cloudIssue"))
            }
        }
    }

    if (confirmBinding) {
        CloudConfirmation(
            R.string.cloud_bind,
            stringResource(if (status?.binding == CloudBinding.OtherAccount) R.string.cloud_rebind_explained else R.string.cloud_bind_explained),
            R.string.cloud_bind,
            confirm = { confirmBinding = false; viewModel.bind() },
            dismiss = { confirmBinding = false },
        )
    }
    state.review?.let { review ->
        val text = review.snapshot?.let { stringResource(R.string.cloud_replace_explained, cloudDate(it.time)) }
            ?: stringResource(R.string.cloud_empty_review)
        CloudConfirmation(
            R.string.cloud_review, text, R.string.cloud_send_new,
            confirm = viewModel::confirmReplacement, dismiss = viewModel::dismissReview,
        )
    }
    state.restoreSelection?.snapshot?.let { snapshot ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRestore,
            title = {
                DismissibleHeading(stringResource(R.string.cloud_restore_title), onClose = viewModel::dismissRestore)
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cloud_restore_ack, cloudDate(snapshot.time)))
                    Text(stringResource(R.string.cloud_restore_expiry, cloudDate(snapshot.expires)))
                    Text(stringResource(R.string.cloud_revision))
                    Text(snapshot.revision, Modifier.testTag("cloudRestoreRevision"),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
                    Text(stringResource(R.string.cloud_restore_explained))
                }
            },
            confirmButton = {
                TextButton(viewModel::confirmRestore, Modifier.heightIn(min = 48.dp).testTag("cloudConfirmRestore")) {
                    Text(stringResource(R.string.cloud_restore_confirm))
                }
            },
            properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        )
    }
    recovery?.let { code ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.cloud_recovery_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.cloud_recovery_note))
                    Text(code, Modifier.testTag("cloudRecoveryCode"),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
                }
            },
            confirmButton = {
                TextButton(viewModel::dismissRecovery, Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cloud_recovery_done))
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false,
                securePolicy = SecureFlagPolicy.SecureOn),
        )
    }
}

@Composable
private fun CloudCredentials(
    enabled: Boolean,
    submit: (String, String, String?) -> Unit,
    recover: (String, String, String) -> Unit,
) {
    // Passwords and single-use codes deliberately bypass rememberSaveable.
    var mode by remember { mutableStateOf(CredentialsMode.SignIn) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    val style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr)
    OutlinedTextField(
        username, { if (it.length <= 32) username = it }, Modifier.fillMaxWidth().testTag("cloudUsername"),
        label = { Text(stringResource(R.string.cloud_username)) }, textStyle = style,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
        singleLine = true, enabled = enabled,
    )
    OutlinedTextField(
        password, { if (it.length <= 256) password = it }, Modifier.fillMaxWidth().testTag("cloudPassword"),
        label = {
            Text(stringResource(if (mode == CredentialsMode.Recover) R.string.cloud_new_password else R.string.cloud_password))
        }, textStyle = style,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = enabled,
    )
    if (mode != CredentialsMode.SignIn) {
        OutlinedTextField(
            invite, { if (it.length <= 32) invite = it }, Modifier.fillMaxWidth().testTag("cloudInvite"),
            label = {
                Text(stringResource(if (mode == CredentialsMode.Recover) R.string.cloud_recovery_title else R.string.cloud_invite))
            }, textStyle = style,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = enabled,
        )
    }
    Button(
        {
            if (mode == CredentialsMode.Recover) recover(username, password, invite)
            else submit(username, password, invite.takeIf { mode == CredentialsMode.Register })
            password = ""
            invite = ""
        },
        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cloudSubmit"),
        enabled = enabled,
    ) { Text(stringResource(mode.label)) }
    val alternative = if (mode == CredentialsMode.SignIn) CredentialsMode.Register else CredentialsMode.SignIn
    TextButton(
        { mode = alternative; invite = ""; password = "" },
        Modifier.heightIn(min = 48.dp).testTag("cloudToggleRegistration"), enabled = enabled,
    ) { Text(stringResource(alternative.label)) }
    if (mode == CredentialsMode.SignIn) {
        TextButton(
            { mode = CredentialsMode.Recover; invite = ""; password = "" },
            Modifier.heightIn(min = 48.dp).testTag("cloudToggleRecovery"), enabled = enabled,
        ) { Text(stringResource(R.string.cloud_recover_account)) }
    }
}

private enum class CredentialsMode(val label: Int) {
    SignIn(R.string.cloud_sign_in),
    Register(R.string.cloud_register),
    Recover(R.string.cloud_recover_account),
}

@Composable
private fun CloudConfirmation(title: Int, text: String, action: Int, confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { DismissibleHeading(stringResource(title), onClose = dismiss) },
        text = { Text(text) },
        confirmButton = { TextButton(confirm, Modifier.heightIn(min = 48.dp)) { Text(stringResource(action)) } },
    )
}

private fun cloudDate(value: Instant): String {
    val local = value.atZone(ZoneId.of("Asia/Tehran"))
    val date = JalaliDates.formatGregorian(local.toLocalDate().toString()) ?: local.toLocalDate().toString()
    return "$date، ${local.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)).withPersianDigits()}"
}

private fun issueLabel(issue: CloudIssue): Int = when (issue) {
    CloudIssue.Input -> R.string.cloud_invalid_input
    CloudIssue.Credentials -> R.string.cloud_bad_credentials
    CloudIssue.RegistrationConflict -> R.string.cloud_registration_conflict
    CloudIssue.SignedOut, CloudIssue.Expired -> R.string.cloud_session_expired
    CloudIssue.BindingRequired -> R.string.cloud_bind_explained
    CloudIssue.DifferentAccount -> R.string.cloud_other_account
    CloudIssue.PopulatedLedger -> R.string.cloud_populated
    CloudIssue.RateLimited -> R.string.cloud_rate_limited
    CloudIssue.Offline -> R.string.cloud_offline
    CloudIssue.Unconfirmed -> R.string.cloud_unconfirmed
    CloudIssue.Conflict -> R.string.cloud_conflict
    CloudIssue.Rejected, CloudIssue.InvalidSnapshot -> R.string.cloud_invalid_snapshot
    CloudIssue.Storage -> R.string.cloud_storage
    CloudIssue.NoSnapshot -> R.string.cloud_restore_missing
    CloudIssue.RestoreChanged -> R.string.cloud_restore_changed
    CloudIssue.DownloadInterrupted -> R.string.cloud_download_interrupted
    CloudIssue.DownloadTooLarge -> R.string.cloud_download_large
    CloudIssue.RestoreInvalid -> R.string.cloud_restore_invalid
    CloudIssue.Restored -> R.string.cloud_restored
    CloudIssue.RestoredWithWarning -> R.string.cloud_restored_warning
    CloudIssue.Uploaded -> R.string.cloud_uploaded
}
