package org.unishiasy.vehiclelog.ui.vehicle

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalog
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalogEntry
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.ui.theme.VehicleLogTheme

@RunWith(AndroidJUnit4::class)
class VehicleSetupTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val acceptanceFontScale = InstrumentationRegistry.getArguments().getString("acceptanceFontScale")
        ?.toFloat()
    private val store = ViewModelStore()
    private lateinit var database: VehicleDatabase
    private var activity: MainActivity? = null

    @After
    fun closeTestStorage() {
        instrumentation.runOnMainSync {
            activity?.finish()
            store.clear()
        }
        if (::database.isInitialized) database.close()
    }

    @Test
    fun addsAndEditsVehicleWithoutChangingId() {
        openScreen()
        assertVisible(R.string.vehicle_empty)
        click(R.string.vehicle_add)
        compose.onNodeWithText(context.getString(R.string.vehicle_catalog_select)).assertDoesNotExist()
        val originalName = "سمند LX"
        compose.onNodeWithTag("vehicleName").performTextInput(originalName)
        compose.onNodeWithTag("vehicleYear").performScrollTo().performTextInput("۱٤0۲")
        click(R.string.vehicle_solar_hijri)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)

        val original = runBlocking { database.vehicleDao().observeAll().first().single() }
        assertEquals(originalName, original.displayLabel)
        assertEquals(1402, original.productionYear)
        assertEquals("solar_hijri", original.productionYearCalendar)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(originalName).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(originalName).performScrollTo().performClick()
        assertVisible(R.string.vehicle_edit)
        compose.onNodeWithTag("vehicleYear").assertTextContains("۱۴۰۲")
        compose.onNodeWithText(context.getString(R.string.vehicle_solar_hijri)).assertIsSelected()

        val editedName = "سمند LX شخصی"
        compose.onNodeWithTag("vehicleName").performScrollTo().performTextReplacement(editedName)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)
        val edited = runBlocking { database.vehicleDao().observeAll().first().single() }
        assertEquals(original.copy(displayLabel = editedName), edited)
    }

    @Test
    fun showsPersianValidationAndKeepsDraftAtLargerFontScale() {
        openScreen(fontScale = 1.5f)
        click(R.string.vehicle_add)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_name_required)

        val name = "پژو ۲۰۶"
        compose.onNodeWithTag("vehicleName").performScrollTo().performTextInput(name)
        compose.onNodeWithTag("vehicleYear").performScrollTo().performTextInput("۱۴۰")
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_calendar_required)
        click(R.string.vehicle_gregorian)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_year_invalid)

        compose.onNodeWithTag("vehicleName").assertTextContains(name)
        compose.onNodeWithTag("vehicleYear").assertTextContains("۱۴۰")
        compose.onNodeWithText(context.getString(R.string.vehicle_saved)).assertDoesNotExist()
        assertTrue(runBlocking { database.vehicleDao().observeAll().first().isEmpty() })
    }

    @Test
    fun selectsMixedScriptMatchAndKeepsDraftAndErrorThroughPicker() {
        val entry = VehicleCatalogEntry("fixture-kite", "آزمایشی خیال‌گرد", "بادبادک ۲۰۳", "VX", listOf("Sky Kite"))
        openScreen(fontScale = 1.5f, catalog = VehicleCatalog(listOf(entry)))
        click(R.string.vehicle_add)
        val name = compose.onNodeWithTag("vehicleName")
        name.performTextInput("خودروی دستی")
        compose.onNodeWithTag("vehicleYear").performScrollTo().performTextInput("۱۴۰")
        click(R.string.vehicle_solar_hijri)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_year_invalid)

        click(R.string.vehicle_catalog_select)
        compose.onNodeWithTag("vehicleCatalogQuery").performTextInput("ناموجود")
        compose.onNodeWithText(context.getString(R.string.vehicle_catalog_no_matches)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.vehicle_catalog_close)).performClick()
        name.assertTextContains("خودروی دستی")
        assertVisible(R.string.vehicle_year_invalid)

        click(R.string.vehicle_catalog_select)
        compose.onNodeWithTag("vehicleCatalogQuery").performTextInput("kite بادبادك ٢0۳")
        compose.onNodeWithText("آزمایشی خیال‌گرد بادبادک ۲۰۳ VX").assertIsDisplayed().performClick()
        name.assertTextContains("آزمایشی خیال‌گرد بادبادک ۲۰۳ VX")
        assertVisible(R.string.vehicle_year_invalid)
        compose.onNodeWithTag("vehicleYear").performTextReplacement("۱۴۰۲")
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)

        val saved = runBlocking { database.vehicleDao().observeAll().first().single() }
        assertEquals(entry.key, saved.catalogKey)
        assertEquals(entry.displayLabel, saved.displayLabel)
        assertEquals(1402, saved.productionYear)
        assertEquals("solar_hijri", saved.productionYearCalendar)
    }

    @Test
    fun editsStoredLabelWhenCatalogEntryIsMissing() {
        val saved = VehicleEntity("saved-id", "car", "نام ذخیره‌شده VX", "retired-entry", null, null)
        val other = VehicleCatalogEntry("fixture-other", "آزمایشی ابرک", "فانوس")
        openScreen(catalog = VehicleCatalog(listOf(other)), savedVehicle = saved)
        compose.onNodeWithText(saved.displayLabel).performClick()
        val name = compose.onNodeWithTag("vehicleName")
        name.assertTextContains(saved.displayLabel)
        val newLabel = "نام دلخواه VX"
        name.performTextReplacement(newLabel)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)

        val updated = runBlocking { database.vehicleDao().observeAll().first().single() }
        assertEquals(saved.copy(displayLabel = newLabel), updated)
    }

    @Test
    fun normalActivityShowsManualFormWithoutCatalog() {
        launchActivity()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(context.getString(R.string.vehicle_add)).fetchSemanticsNodes().isNotEmpty()
        }
        click(R.string.vehicle_add)
        compose.onNodeWithTag("vehicleName").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.vehicle_catalog_select)).assertDoesNotExist()
    }

    @Test
    fun reviewsManualFormOnDevice() {
        openScreen(fontScale = acceptanceFontScale)
        click(R.string.vehicle_add)
        compose.onNodeWithText(context.getString(R.string.vehicle_catalog_select)).assertDoesNotExist()
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_name_required)
        capture("manual-name-error")

        val label = "خودروی آزمایشی VX"
        val name = compose.onNodeWithTag("vehicleName")
        val year = compose.onNodeWithTag("vehicleYear")
        name.performScrollTo().performTouchInput { click() }.performTextInput(label)
        year.performScrollTo().performTouchInput { click() }.performTextInput("۱۴۰")
        compose.waitUntil(5_000) { keyboardShown(year) }
        assertTouchTarget(R.string.vehicle_save)
        assertTrue(keyboardShown(year))
        assertTouchTarget(compose.onNodeWithTag("vehicleCancel"), scroll = false)
        assertTrue(keyboardShown(year))
        capture("manual-actions")
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_calendar_required)
        assertTouchTarget(R.string.vehicle_solar_hijri)
        click(R.string.vehicle_solar_hijri)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_year_invalid)
        name.assertTextContains(label)
        year.assertTextContains("۱۴۰")
        compose.onNodeWithText(context.getString(R.string.vehicle_saved)).assertDoesNotExist()
        assertTrue(runBlocking { database.vehicleDao().observeAll().first().isEmpty() })
        capture("manual-year-error")

        year.performScrollTo().performTextReplacement("۱۴۰۲")
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)
        val saved = runBlocking { database.vehicleDao().observeAll().first().single() }
        assertEquals(label, saved.displayLabel)
        assertEquals(1402, saved.productionYear)
        assertEquals("solar_hijri", saved.productionYearCalendar)
        compose.onNodeWithText(label).assertIsDisplayed()
        capture("manual-list")
        compose.onNodeWithText(label).performClick()
        assertVisible(R.string.vehicle_edit)
        year.assertTextContains("۱۴۰۲")
        capture("manual-edit")
        name.performScrollTo().performTextReplacement("$label ویرایش")
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)
        assertEquals(saved.copy(displayLabel = "$label ویرایش"), runBlocking {
            database.vehicleDao().observeAll().first().single()
        })
    }

    @Test
    fun reviewsCatalogPickerOnDevice() {
        val entry = VehicleCatalogEntry("fixture-kite", "آزمایشی خیال‌گرد", "بادبادک ۲۰۳", "VX", listOf("Sky Kite"))
        val retired = VehicleEntity("saved-id", "car", "نام ذخیره‌شده VX", "retired-entry")
        openScreen(fontScale = acceptanceFontScale, catalog = VehicleCatalog(listOf(entry)), savedVehicle = retired)
        compose.onNodeWithText(retired.displayLabel).performClick()
        val name = compose.onNodeWithTag("vehicleName")
        val year = compose.onNodeWithTag("vehicleYear")
        name.assertTextContains(retired.displayLabel)
        capture("catalog-retired-edit")
        name.performTextReplacement("نام دلخواه VX")
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)
        assertEquals(retired.copy(displayLabel = "نام دلخواه VX"), runBlocking {
            database.vehicleDao().observeAll().first().single()
        })

        click(R.string.vehicle_add)
        name.performTextInput("خودروی دستی")
        year.performScrollTo().performTextInput("۱۴۰")
        click(R.string.vehicle_solar_hijri)
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_year_invalid)
        click(R.string.vehicle_catalog_select)
        val search = compose.onNodeWithTag("vehicleCatalogQuery")
        search.performTouchInput { click() }.performTextInput("ناموجود")
        compose.waitUntil(5_000) { keyboardShown(search) }
        compose.onNodeWithText(context.getString(R.string.vehicle_catalog_no_matches)).assertIsDisplayed()
        assertTouchTarget(R.string.vehicle_catalog_close, scroll = false)
        capture("catalog-no-results")
        compose.onNodeWithText(context.getString(R.string.vehicle_catalog_close)).performClick()
        assertPickerDraft("خودروی دستی")

        click(R.string.vehicle_catalog_select)
        search.performTouchInput { click() }.performTextInput("ناموجود")
        compose.waitUntil(5_000) { keyboardShown(search) }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { !keyboardShown(search) }
        search.assertIsDisplayed()
        capture("catalog-before-back")
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("vehicleCatalogQuery").fetchSemanticsNodes().isEmpty()
        }
        assertPickerDraft("خودروی دستی")

        click(R.string.vehicle_catalog_select)
        search.performTouchInput { click() }.performTextInput("بادبادک")
        val result = compose.onNodeWithText(entry.displayLabel)
        result.assertIsDisplayed().assertHasClickAction()
        search.performTextReplacement("kite بادبادك ٢0۳")
        result.assertIsDisplayed()
        compose.waitUntil(5_000) { keyboardShown(search) }
        File(evidenceDirectory(), "catalog-font-scale.txt").writeText(
            checkNotNull(search.fetchSemanticsNode().root).density.fontScale.toString(),
        )
        capture("catalog-match")
        result.performTouchInput { click() }
        assertPickerDraft(entry.displayLabel)
        year.performScrollTo().performTextReplacement("۱۴۰۲")
        click(R.string.vehicle_save)
        assertVisible(R.string.vehicle_saved)
        val saved = runBlocking { database.vehicleDao().observeAll().first().single { it.id != retired.id } }
        assertEquals(entry.displayLabel, saved.displayLabel)
        assertEquals(entry.key, saved.catalogKey)
        assertEquals(1402, saved.productionYear)
        assertEquals("solar_hijri", saved.productionYearCalendar)
        compose.onNodeWithText(entry.displayLabel).performScrollTo().performClick()
        name.assertTextContains(entry.displayLabel)
    }

    private fun assertPickerDraft(label: String) {
        compose.onNodeWithTag("vehicleName").assertTextContains(label)
        compose.onNodeWithTag("vehicleYear").assertTextContains("۱۴۰")
        compose.onNodeWithText(context.getString(R.string.vehicle_solar_hijri)).assertIsSelected()
        assertVisible(R.string.vehicle_year_invalid)
    }

    private fun assertTouchTarget(id: Int, scroll: Boolean = true) =
        assertTouchTarget(compose.onNodeWithText(context.getString(id)), scroll)

    private fun assertTouchTarget(node: SemanticsNodeInteraction, scroll: Boolean) {
        if (scroll) node.performScrollTo()
        val bounds = node.assertIsDisplayed().assertHasClickAction().fetchSemanticsNode().touchBoundsInRoot
        val minimum = 48 * context.resources.displayMetrics.density
        assertTrue("Expected a touch target of at least 48 dp: $bounds",
            bounds.width >= minimum && bounds.height >= minimum)
    }

    private fun keyboardShown(node: SemanticsNodeInteraction): Boolean {
        val root = node.fetchSemanticsNode().root as ViewRootForTest
        return compose.runOnUiThread {
            ViewCompat.getRootWindowInsets(root.view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
    }

    private fun evidenceDirectory(): File {
        val scale = acceptanceFontScale ?: context.resources.configuration.fontScale
        return File(context.cacheDir, "m25-acceptance-insets/$scale").apply { mkdirs() }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(evidenceDirectory(), "$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun launchActivity() {
        val component = ComponentName(context, MainActivity::class.java)
        // Launch from the shell because MIUI can block a background ActivityScenario start.
        val command = "am start -W -a ${Intent.ACTION_MAIN} -c ${Intent.CATEGORY_LAUNCHER} " +
            "-n ${component.flattenToString()}"
        val launch = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }
        assertTrue(launch, launch.lineSequence().any { it.trim() == "Status: ok" })
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().single()
        }
    }

    private fun openScreen(
        fontScale: Float? = null,
        catalog: VehicleCatalog? = null,
        savedVehicle: VehicleEntity? = null,
    ) {
        launchActivity()
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).build()
        if (savedVehicle != null) runBlocking { database.vehicleDao().insert(savedVehicle) }
        instrumentation.runOnMainSync {
            val factory = viewModelFactory {
                initializer { VehicleViewModel(VehicleRepository(database.vehicleDao())) { catalog } }
            }
            val model = ViewModelProvider.create(store, factory)[VehicleViewModel::class]
            checkNotNull(activity).setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(density.density, fontScale ?: density.fontScale),
                ) {
                    VehicleLogTheme { VehicleScreen(model) }
                }
            }
        }
        val visibleText = savedVehicle?.displayLabel ?: context.getString(R.string.vehicle_empty)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(visibleText).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(id: Int) {
        compose.onNodeWithText(context.getString(id)).performScrollTo().performClick()
    }

    private fun assertVisible(id: Int) {
        compose.onNodeWithText(context.getString(id)).performScrollTo().assertIsDisplayed()
    }
}
