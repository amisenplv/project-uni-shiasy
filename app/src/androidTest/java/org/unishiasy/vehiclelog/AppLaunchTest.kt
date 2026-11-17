package org.unishiasy.vehiclelog

import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun homeDisplaysPersianTextInRightToLeftLayout() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val component = ComponentName(instrumentation.targetContext, MainActivity::class.java)
        // MIUI can deny ActivityScenario's background start before the test body runs.
        val command = "am start -W -a ${Intent.ACTION_MAIN} -c ${Intent.CATEGORY_LAUNCHER} " +
            "-n ${component.flattenToString()}"
        val launchResult = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }
        assertTrue(
            "Launcher start did not succeed:\n$launchResult",
            launchResult.lineSequence().any { it.trim() == "Status: ok" },
        )

        val title = compose.onNodeWithText("دفتر خودرو")
        title.assertIsDisplayed()
        compose.onNodeWithText("مدیریت هزینه‌ها و سرویس‌های خودرو").assertIsDisplayed()

        val titleLayout = title.fetchSemanticsNode().layoutInfo
        compose.runOnIdle {
            assertEquals(LayoutDirection.Rtl, titleLayout.layoutDirection)
        }
    }
}
