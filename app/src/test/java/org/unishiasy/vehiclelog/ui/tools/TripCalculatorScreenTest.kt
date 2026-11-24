package org.unishiasy.vehiclelog.ui.tools

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.domain.PersianDigits
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TripCalculatorScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

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
    fun rendersAllInputFieldsAndSummaryCards() {
        setTestContent {
            TripCalculatorScreen()
        }

        compose.onNodeWithTag("tripCalculatorScreen").assertIsDisplayed()
        compose.onNodeWithTag("tripDistanceInput").assertIsDisplayed()
        compose.onNodeWithTag("tripRateInput").assertIsDisplayed()
        compose.onNodeWithTag("tripConsumptionInput").assertIsDisplayed()
        compose.onNodeWithTag("tripTotalFuel").assertIsDisplayed()
        compose.onNodeWithTag("tripTotalCost").assertIsDisplayed()

        // Missing inputs display prompts instead of numeric zeros
        compose.onNodeWithTag("tripTotalFuel").assertTextContains("نیازمند مسافت و مصرف", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("اطلاعات سفر را وارد کنید", substring = true)
    }

    @Test
    fun calculatesTripCostAndFuelCorrectly() {
        setTestContent {
            TripCalculatorScreen()
        }

        // 500 km, 8.0 L/100km, 3000 Toman/L
        // Expected Fuel: (500 * 8.0) / 100 = 40.0 Liters
        // Expected Cost: 40.0 * 3000 = 120,000 Toman
        compose.onNodeWithTag("tripDistanceInput").performTextReplacement("500")
        compose.onNodeWithTag("tripConsumptionInput").performTextReplacement("8.0")
        compose.onNodeWithTag("tripRateInput").performTextReplacement("3000")

        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("۴۰", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("۱۲۰٬۰۰۰", substring = true)
    }

    @Test
    fun handlesZeroDistanceGracefully() {
        setTestContent {
            TripCalculatorScreen()
        }

        compose.onNodeWithTag("tripDistanceInput").performTextReplacement("0")
        compose.onNodeWithTag("tripConsumptionInput").performTextReplacement("8.0")
        compose.onNodeWithTag("tripRateInput").performTextReplacement("3000")

        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("مسافت نامعتبر است", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("اطلاعات سفر را وارد کنید", substring = true)
    }

    @Test
    fun handlesInvalidRateGracefully() {
        setTestContent {
            TripCalculatorScreen()
        }

        // Valid distance and consumption, invalid rate
        compose.onNodeWithTag("tripDistanceInput").performTextReplacement("500")
        compose.onNodeWithTag("tripConsumptionInput").performTextReplacement("8.0")
        compose.onNodeWithTag("tripRateInput").performTextReplacement("abc")

        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("۴۰", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("نرخ سوخت نامعتبر است", substring = true)
    }

    @Test
    fun distinguishIncompleteEstimateFromCalculatedCostAcrossAllStates() {
        setTestContent {
            TripCalculatorScreen()
        }

        // 1. Empty on open: asserts prompts
        compose.onNodeWithTag("tripTotalFuel").assertTextContains("نیازمند مسافت و مصرف", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("اطلاعات سفر را وارد کنید", substring = true)

        // 2. Enter distance 100 and consumption 8: asserts 8 liters fuel result and prompt for missing rate
        compose.onNodeWithTag("tripDistanceInput").performTextReplacement("۱۰۰")
        compose.onNodeWithTag("tripConsumptionInput").performTextReplacement("۸")
        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("۸", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("نرخ سوخت را وارد کنید", substring = true)

        // 3. Enter rate 3,000: asserts 8 liters and 24,000 Toman
        compose.onNodeWithTag("tripRateInput").performTextReplacement("۳۰۰۰")
        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("۸", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("۲۴٬۰۰۰", substring = true)

        // 4. Clear rate: asserts 8 liters remains, cost displays prompt for rate
        compose.onNodeWithTag("tripRateInput").performTextReplacement("")
        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("۸", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("نرخ سوخت را وارد کنید", substring = true)

        // 5. Invalid rate (e.g. "abc"): asserts 8 liters remains, cost displays invalid rate prompt
        compose.onNodeWithTag("tripRateInput").performTextReplacement("abc")
        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("۸", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("نرخ سوخت نامعتبر است", substring = true)

        // 6. Reset: asserts inputs cleared and summary cards return to prompt state
        compose.onNodeWithTag("tripResetButton").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("نیازمند مسافت و مصرف", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("اطلاعات سفر را وارد کنید", substring = true)
    }

    @Test
    fun normalizesPersianDigitsOnInput() {
        setTestContent {
            TripCalculatorScreen()
        }

        // Entering Persian digits "۵۰۰", "۸", "۳۰۰۰"
        compose.onNodeWithTag("tripDistanceInput").performTextReplacement("۵۰۰")
        compose.onNodeWithTag("tripConsumptionInput").performTextReplacement("۸")
        compose.onNodeWithTag("tripRateInput").performTextReplacement("۳۰۰۰")

        compose.waitForIdle()

        compose.onNodeWithTag("tripTotalFuel").assertTextContains("۴۰", substring = true)
        compose.onNodeWithTag("tripTotalCost").assertTextContains("۱۲۰٬۰۰۰", substring = true)
    }

    @Test
    fun quickPresetsUpdateRatePerLiter() {
        setTestContent {
            TripCalculatorScreen()
        }

        compose.onNodeWithTag("tripDistanceInput").performTextReplacement("100")
        compose.onNodeWithTag("tripConsumptionInput").performTextReplacement("10")

        // Click preset 1500 (سهمیه‌ای)
        compose.onNodeWithTag("ratePreset:1500").performScrollTo().performClick()
        compose.waitForIdle()

        // 100km * 10L/100km = 10L -> 10 * 1500 = 15,000 Toman
        compose.onNodeWithTag("tripTotalCost").assertTextContains("۱۵٬۰۰۰", substring = true)

        // Click preset 3000 (آزاد)
        compose.onNodeWithTag("ratePreset:3000").performScrollTo().performClick()
        compose.waitForIdle()

        // 10L * 3000 = 30,000 Toman
        compose.onNodeWithTag("tripTotalCost").assertTextContains("۳۰٬۰۰۰", substring = true)
    }

    @Test
    fun touchTargetsSatisfyAccessibilityConstraint() {
        setTestContent {
            TripCalculatorScreen()
        }

        compose.onNodeWithTag("tripDistanceInput").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("tripRateInput").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("tripConsumptionInput").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("ratePreset:1500").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("ratePreset:3000").assertHeightIsAtLeast(48.dp)
    }
}
