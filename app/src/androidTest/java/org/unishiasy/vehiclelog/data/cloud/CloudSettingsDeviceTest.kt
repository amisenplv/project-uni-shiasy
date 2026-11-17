package org.unishiasy.vehiclelog.data.cloud

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Rule
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.BuildConfig
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R

@RunWith(AndroidJUnit4::class)
class CloudSettingsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun requireLocalFixture() {
        assumeTrue(BuildConfig.LOCAL_CLOUD_ENABLED &&
            InstrumentationRegistry.getArguments().getString("localCloudFixture") == "true")
    }

    @Test fun settingsShowsPersianAccountOptInAndOneTimeRecoveryCode() {
        val invite = checkNotNull(InstrumentationRegistry.getArguments().getString("invite"))
        compose.onNodeWithText(compose.activity.getString(R.string.backup_settings)).performClick()
        compose.onNodeWithTag("backupExport").assertIsDisplayed()
        compose.onNodeWithTag("settingsCloud").performScrollTo().performClick()
        waitFor("cloudToggleRegistration")
        compose.onNodeWithTag("cloudToggleRegistration").performScrollTo().performClick()
        compose.onNodeWithTag("cloudUsername").performScrollTo()
            .performTextInput("m53_ui_" + UUID.randomUUID().toString().take(8))
        compose.onNodeWithTag("cloudPassword").performScrollTo().performTextInput("synthetic UI fixture password")
        compose.onNodeWithTag("cloudInvite").performScrollTo().performTextInput(invite)
        compose.onNodeWithTag("cloudSubmit").performScrollTo().performClick()
        waitFor("cloudRecoveryCode")
        compose.onNodeWithTag("cloudRecoveryCode").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.cloud_recovery_done)).performClick()
        compose.onNodeWithTag("cloudBind").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("cloudSignOut").performScrollTo().performClick()
        waitFor("cloudUsername")
        compose.onNodeWithTag("cloudUsername").assertIsDisplayed()
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(12_000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
    }
}
