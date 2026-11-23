package org.unishiasy.vehiclelog.ui.components

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FloatingDockTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val sampleIcon = ImageVector.Builder(
        name = "SampleIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).build()

    private fun setTestContent(content: @Composable () -> Unit) {
        val act = compose.activity
        act.runOnUiThread {
            act.setContent {
                VehicleLogTheme {
                    content()
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun floatingBottomDockRendersAllTabsAndSelectsTab() {
        var selectedTab by mutableStateOf(MainTab.HOME)

        setTestContent {
            FloatingBottomDock(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }

        compose.onNodeWithTag("floatingBottomDock").assertIsDisplayed()
        compose.onNodeWithTag("dockTab:home").assertIsDisplayed()
        compose.onNodeWithTag("dockTab:history").assertIsDisplayed()
        compose.onNodeWithTag("dockTab:reminders").assertIsDisplayed()
        compose.onNodeWithTag("dockTab:tools").assertIsDisplayed()

        compose.onNodeWithText("خانه").assertIsDisplayed()
        compose.onNodeWithText("سوابق").assertIsDisplayed()
        compose.onNodeWithText("یادآورها").assertIsDisplayed()
        compose.onNodeWithText("ابزارها").assertIsDisplayed()

        compose.onNodeWithTag("dockTab:tools").performClick()
        assertEquals(MainTab.TOOLS, selectedTab)

        compose.onNodeWithTag("dockTab:history").performClick()
        assertEquals(MainTab.HISTORY, selectedTab)
    }

    @Test
    fun metricCardRendersTitleValueAndSubtitle() {
        setTestContent {
            MetricCard(
                title = "مصرف میانگین",
                value = "۷٫۲ لیتر",
                subtitle = "در ۱۰۰ کیلومتر",
                icon = sampleIcon,
            )
        }

        compose.onNodeWithText("مصرف میانگین").assertIsDisplayed()
        compose.onNodeWithText("۷٫۲ لیتر").assertIsDisplayed()
        compose.onNodeWithText("در ۱۰۰ کیلومتر").assertIsDisplayed()
    }

    @Test
    fun statusPillRendersGivenText() {
        setTestContent {
            StatusPill(
                text = "باک پر",
                containerColor = Color.DarkGray,
                contentColor = Color.White,
            )
        }

        compose.onNodeWithText("باک پر").assertIsDisplayed()
    }

    @Test
    fun circularIconBadgeRendersWithoutCrashing() {
        setTestContent {
            CircularIconBadge(
                icon = sampleIcon,
                backgroundColor = Color.DarkGray,
                iconColor = Color.White,
            )
        }
    }
}
