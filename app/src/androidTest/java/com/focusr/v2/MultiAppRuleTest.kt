package com.focusr.v2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.DayOfWeek
import com.focusr.v2.models.RuleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Comprehensive tests for multi-app rule system with precedence
 */
@RunWith(AndroidJUnit4::class)
class MultiAppRuleTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var blockingTimeManager: BlockingTimeManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        preferencesManager = PreferencesManager(context)
        blockingTimeManager = BlockingTimeManager(preferencesManager)
        
        // Clear all existing rules
        val existingRules = preferencesManager.blockingRules.first()
        existingRules.forEach { preferencesManager.removeRule(it.id) }
    }

    // ========== MULTI-APP RULE TESTS ==========

    @Test
    fun testMultiAppRuleCreation() {
        val rule = BlockingRule(
            name = "Social Media Block",
            packageNames = listOf(
                "com.instagram.android",
                "com.facebook.katana",
                "com.twitter.android"
            ),
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(18, 0)
        )

        assertTrue("Multi-app rule should be valid", rule.isValid())
        assertEquals("Should have 3 apps", 3, rule.getApps().size)
        assertTrue("Should contain Instagram", rule.getApps().contains("com.instagram.android"))
        assertEquals("App count description should be correct", "3 apps", rule.getAppCountDescription())
    }

    @Test
    fun testSingleAppRuleStillWorks() {
        val rule = BlockingRule(
            name = "YouTube Block",
            packageNames = listOf("com.youtube.android"),
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(20, 0)
        )

        assertTrue("Single app rule should be valid", rule.isValid())
        assertEquals("Should have 1 app", 1, rule.getApps().size)
        assertEquals("App count description should be correct", "1 app", rule.getAppCountDescription())
    }

    @Test
    fun testBackwardCompatibility() {
        // Old-style rule with deprecated packageName field
        @Suppress("DEPRECATION")
        val oldRule = BlockingRule(
            packageName = "com.instagram.android",
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(18, 0)
        )

        assertTrue("Old rule should still be valid", oldRule.isValid())
        assertEquals("getApps() should return old packageName", 1, oldRule.getApps().size)
        assertEquals("Should contain old package", "com.instagram.android", oldRule.getApps().first())
    }

    @Test
    fun testRuleDisplayName() {
        val namedRule = BlockingRule(
            name = "Morning Focus",
            packageNames = listOf("com.instagram.android"),
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(9, 0)
        )
        assertEquals("Should use provided name", "Morning Focus", namedRule.getDisplayName())

        val unnamedRule = BlockingRule(
            packageNames = listOf("com.instagram.android", "com.facebook.katana"),
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(9, 0)
        )
        assertEquals("Should auto-generate name", "Rule for 2 apps", unnamedRule.getDisplayName())
    }

    @Test
    fun testCalculatePriority() {
        val simpleRule = BlockingRule(
            packageNames = listOf("com.test"),
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(18, 0)
        )
        assertEquals("SIMPLE rule should have priority 1", 1, simpleRule.calculatePriority())

        val scheduledRule = BlockingRule(
            packageNames = listOf("com.test"),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(9, 0),
            toTime = Pair(17, 0)
        )
        assertEquals("SCHEDULED rule should have priority 2", 2, scheduledRule.calculatePriority())
    }

    // ========== PRECEDENCE TESTS ==========

    @Test
    fun testPrecedence_SimpleOverScheduled() = runBlocking {
        val packageName = "com.youtube.android"
        
        // Create SCHEDULED rule (priority 2)
        val scheduledRule = BlockingRule(
            name = "Night Block",
            packageNames = listOf(packageName),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(0, 0),   // Midnight
            toTime = Pair(23, 59),   // 11:59 PM (active all day)
            daysOfWeek = DayOfWeek.values().toSet()
        )
        
        // Create SIMPLE rule (priority 1) - blocks until 6 PM
        val simpleRule = BlockingRule(
            name = "Quick Block",
            packageNames = listOf(packageName),
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(18, 0)
        )
        
        preferencesManager.addRule(scheduledRule)
        preferencesManager.addRule(simpleRule)
        
        // Get current hour
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // If before 6 PM, SIMPLE rule should be active (takes precedence)
        // If after 6 PM, SCHEDULED rule should be active
        val shouldBlock = blockingTimeManager.shouldBlockApp(packageName)
        
        if (currentHour < 18) {
            assertTrue("Should block before 6 PM (SIMPLE rule active)", shouldBlock)
        } else {
            assertTrue("Should block after 6 PM (SCHEDULED rule active)", shouldBlock)
        }
    }

    @Test
    fun testMultiAppRuleBlocksAllApps() = runBlocking {
        val rule = BlockingRule(
            name = "Social Media Block",
            packageNames = listOf(
                "com.instagram.android",
                "com.facebook.katana",
                "com.twitter.android"
            ),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(0, 0),
            toTime = Pair(23, 59),
            daysOfWeek = DayOfWeek.values().toSet()
        )
        
        preferencesManager.addRule(rule)
        
        // All apps in the rule should be blocked
        assertTrue("Instagram should be blocked", blockingTimeManager.shouldBlockApp("com.instagram.android"))
        assertTrue("Facebook should be blocked", blockingTimeManager.shouldBlockApp("com.facebook.katana"))
        assertTrue("Twitter should be blocked", blockingTimeManager.shouldBlockApp("com.twitter.android"))
        
        // App not in rule should not be blocked
        assertFalse("YouTube should not be blocked", blockingTimeManager.shouldBlockApp("com.youtube.android"))
    }

    @Test
    fun testGetCurrentlyBlockedApps_MultiApp() = runBlocking {
        val rule = BlockingRule(
            name = "Test Block",
            packageNames = listOf(
                "com.app1",
                "com.app2",
                "com.app3"
            ),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(0, 0),
            toTime = Pair(23, 59),
            daysOfWeek = DayOfWeek.values().toSet()
        )
        
        preferencesManager.addRule(rule)
        
        val blockedApps = blockingTimeManager.getCurrentlyBlockedApps()
        
        assertEquals("Should have 3 blocked apps", 3, blockedApps.size)
        assertTrue("Should contain app1", blockedApps.contains("com.app1"))
        assertTrue("Should contain app2", blockedApps.contains("com.app2"))
        assertTrue("Should contain app3", blockedApps.contains("com.app3"))
    }

    @Test
    fun testMultipleRulesForSameApp_Precedence() = runBlocking {
        val packageName = "com.test.app"
        
        // Rule 1: SCHEDULED (priority 2) - active all day
        val scheduledRule = BlockingRule(
            name = "All Day Block",
            packageNames = listOf(packageName),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(0, 0),
            toTime = Pair(23, 59),
            daysOfWeek = DayOfWeek.values().toSet()
        )
        
        // Rule 2: SIMPLE (priority 1) - blocks until 6 PM
        val simpleRule = BlockingRule(
            name = "Until Evening",
            packageNames = listOf(packageName),
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(18, 0)
        )
        
        preferencesManager.addRule(scheduledRule)
        preferencesManager.addRule(simpleRule)
        
        // Should check SIMPLE rule first due to precedence
        val shouldBlock = blockingTimeManager.shouldBlockApp(packageName)
        
        // Should be blocked regardless of time (one of the rules is always active)
        assertTrue("App should be blocked by one of the rules", shouldBlock)
    }

    @Test
    fun testDisabledRulesNotApplied() = runBlocking {
        val rule = BlockingRule(
            name = "Disabled Rule",
            packageNames = listOf("com.test.app"),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(0, 0),
            toTime = Pair(23, 59),
            daysOfWeek = DayOfWeek.values().toSet(),
            enabled = false  // Disabled
        )
        
        preferencesManager.addRule(rule)
        
        assertFalse("Disabled rule should not block app", blockingTimeManager.shouldBlockApp("com.test.app"))
    }

    @Test
    fun testEmptyPackageNamesInvalid() {
        val rule = BlockingRule(
            name = "Invalid Rule",
            packageNames = emptyList(),  // No apps
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(18, 0)
        )
        
        assertFalse("Rule with no apps should be invalid", rule.isValid())
    }

    @Test
    fun testGetActiveRulesCount() = runBlocking {
        // Add active rule
        val activeRule = BlockingRule(
            name = "Active Rule",
            packageNames = listOf("com.test1"),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(0, 0),
            toTime = Pair(23, 59),
            daysOfWeek = DayOfWeek.values().toSet()
        )
        
        // Add inactive rule (wrong day)
        val inactiveRule = BlockingRule(
            name = "Inactive Rule",
            packageNames = listOf("com.test2"),
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(0, 0),
            toTime = Pair(23, 59),
            daysOfWeek = emptySet()  // No days selected
        )
        
        preferencesManager.addRule(activeRule)
        preferencesManager.addRule(inactiveRule)
        
        val activeCount = blockingTimeManager.getActiveRulesCount()
        
        assertTrue("Should have at least 1 active rule", activeCount >= 1)
    }
}
