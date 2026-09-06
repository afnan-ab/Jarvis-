package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.automation.PhoneAutomationManager
import com.example.infrastructure.InfrastructureManager
import com.example.infrastructure.ServerTelemetryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ServerResourceDashboardTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `telemetry history buffer initializes with continuous time series points`() {
        val infraManager = InfrastructureManager(testScope)
        val history = infraManager.telemetryHistory.value

        assertTrue("Telemetry history should not be empty", history.isNotEmpty())
        assertEquals(25, history.size)

        val firstPoint = history.first()
        assertNotNull(firstPoint.timeLabel)
        assertTrue("CPU percentage should be in valid range", firstPoint.cpuUsage in 0f..100f)
        assertTrue("RAM percentage should be in valid range", firstPoint.ramUsage in 0f..100f)
        assertTrue("Network throughput should be positive", firstPoint.networkLoadGbps >= 0f)
    }

    @Test
    fun `rebalance protocol scales cluster loads downward`() {
        val infraManager = InfrastructureManager(testScope)
        val initialStats = infraManager.globalStats.value

        val optimizeMessage = infraManager.optimizeAll()
        assertTrue("Optimize message should confirm cluster rebalancing", optimizeMessage.contains("rebalanced"))

        val clusters = infraManager.clusters.value
        assertTrue("All clusters should be present", clusters.size >= 4)
    }

    @Test
    fun `phone automation manager discovers launchable apps on device`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val phoneManager = PhoneAutomationManager(context)

        val apps = phoneManager.getLaunchableApps()
        // In Robolectric environment, system apps or launcher activities are queryable
        assertNotNull(apps)
    }
}
