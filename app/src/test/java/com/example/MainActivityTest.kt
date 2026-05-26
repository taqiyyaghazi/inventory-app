package com.example

import androidx.test.core.app.ActivityScenario
import com.example.core.data.InventoryDatabase
import com.example.core.viewmodel.InventoryViewModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Use 33 or 34 or 36 matching existing target SDK
class MainActivityTest {

    @Before
    fun setUp() {
        InventoryDatabase.closeDatabase()
    }

    @After
    fun tearDown() {
        InventoryDatabase.closeDatabase()
    }

    @Test
    fun testMainActivityLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
            }
        }
    }

    @Test
    fun testViewModelStateFlowCaches() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = InventoryViewModel(app)

        // Seed some categories / default locations are auto-populated
        assert(viewModel.locationsState.value.isNotEmpty())

        // Add item
        viewModel.addItem(
            name = "Test Item Pro",
            category = "Elektronik",
            quantity = 2,
            location = "Kabinet Tengah",
            value = 150000.0,
            notes = "Barang tes"
        )
    }
}
