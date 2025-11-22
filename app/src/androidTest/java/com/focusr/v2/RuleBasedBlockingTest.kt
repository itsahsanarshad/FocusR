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
 * Test for rule-based blocking system
 */
@RunWith(AndroidJUnit4::class)
class RuleBasedBlockingTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var blockingTimeManager: BlockingTimeManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferencesManager = PreferencesManager(context)
        blockingTimeManager = BlockingTimeManager(preferencesManager)
    }

    @Test
    fun testSimpleRuleCreation() {
        val rule = BlockingRule(
            packageName = "com.instagram.android",
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(22, 0) // Block until 10 PM
        )

        assertTrue("Simple rule should be valid", rule.isValid())
        assertEquals("Rule type should be SIMPLE", RuleType.SIMPLE, rule.ruleType)
        assertEquals("Package name should match", "com.instagram.android", rule.packageName)
    }

    @Test
    fun testScheduledRuleCreation() {
        val rule = BlockingRule(
            packageName = "com.youtube.android",
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(2, 0),  // 2 AM
            toTime = Pair(5, 0),    // 5 AM
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
        )

        assertTrue("Scheduled rule should be valid", rule.isValid())
        assertEquals("Rule type should be SCHEDULED", RuleType.SCHEDULED, rule.ruleType)
        assertEquals("Should have 3 days", 3, rule.daysOfWeek.size)
    }

    @Test
    fun testRuleDescription() {
        val simpleRule = BlockingRule(
            packageName = "com.test",
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(14, 30) // 2:30 PM
        )

        val description = simpleRule.getDescription()
        assertTrue("Description should mention time", description.contains("2:30 PM"))
        assertTrue("Description should mention 'Until'", description.contains("Until"))
    }

    @Test
    fun testAddAndRetrieveRule() = runBlocking {
        // Clear any existing rules first
        val existingRules = preferencesManager.blockingRules.first()
        existingRules.forEach { preferencesManager.removeRule(it.id) }

        val rule = BlockingRule(
            packageName = "com.test.app",
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(20, 0)
        )

        // Add rule
        preferencesManager.addRule(rule)

        // Retrieve rules for the app
        val retrievedRules = preferencesManager.getRulesForApp("com.test.app")

        assertEquals("Should have 1 rule", 1, retrievedRules.size)
        assertEquals("Package name should match", "com.test.app", retrievedRules[0].packageName)
        assertEquals("Rule type should match", RuleType.SIMPLE, retrievedRules[0].ruleType)
    }

    @Test
    fun testMultipleRulesForSameApp() = runBlocking {
        // Clear existing rules
        val existingRules = preferencesManager.blockingRules.first()
        existingRules.forEach { preferencesManager.removeRule(it.id) }

        val rule1 = BlockingRule(
            packageName = "com.test.app",
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(18, 0)
        )

        val rule2 = BlockingRule(
            packageName = "com.test.app",
            ruleType = RuleType.SCHEDULED,
            fromTime = Pair(9, 0),
            toTime = Pair(17, 0),
            daysOfWeek = DayOfWeek.weekdays()
        )

        preferencesManager.addRule(rule1)
        preferencesManager.addRule(rule2)

        val retrievedRules = preferencesManager.getRulesForApp("com.test.app")

        assertEquals("Should have 2 rules", 2, retrievedRules.size)
    }

    @Test
    fun testRemoveRule() = runBlocking {
        val rule = BlockingRule(
            packageName = "com.test.remove",
            ruleType = RuleType.SIMPLE,
            blockUntilTime = Pair(20, 0)
        )

        preferencesManager.addRule(rule)
        
        var rules = preferencesManager.getRulesForApp("com.test.remove")
        assertEquals("Should have 1 rule before removal", 1, rules.size)

        preferencesManager.removeRule(rule.id)

        rules = preferencesManager.getRulesForApp("com.test.remove")
        assertEquals("Should have 0 rules after removal", 0, rules.size)
    }

    @Test
    fun testDayOfWeekHelpers() {
        val weekdays = DayOfWeek.weekdays()
        assertEquals("Should have 5 weekdays", 5, weekdays.size)
        assertTrue("Should contain Monday", weekdays.contains(DayOfWeek.MONDAY))
        assertFalse("Should not contain Saturday", weekdays.contains(DayOfWeek.SATURDAY))

        val weekend = DayOfWeek.weekend()
        assertEquals("Should have 2 weekend days", 2, weekend.size)
        assertTrue("Should contain Saturday", weekend.contains(DayOfWeek.SATURDAY))
        assertTrue("Should contain Sunday", weekend.contains(DayOfWeek.SUNDAY))
    }

    @Test
    fun testCurrentDay() {
        val currentDay = DayOfWeek.getCurrentDay()
        assertNotNull("Current day should not be null", currentDay)
        
        val calendar = Calendar.getInstance()
        val expectedDay = DayOfWeek.fromCalendar(calendar.get(Calendar.DAY_OF_WEEK))
        assertEquals("Current day should match calendar", expectedDay, currentDay)
    }

    @Test
    fun testInvalidRules() {
        // Simple rule without blockUntilTime
        val invalidSimple = BlockingRule(
            packageName = "com.test",
            ruleType = RuleType.SIMPLE,
            blockUntilTime = null
        )
        assertFalse("Invalid simple rule should not be valid", invalidSimple.isValid())

        // Scheduled rule without times
        val invalidScheduled = BlockingRule(
            packageName = "com.test",
            ruleType = RuleType.SCHEDULED,
            fromTime = null,
            toTime = null
        )
        assertFalse("Invalid scheduled rule should not be valid", invalidScheduled.isValid())
    }
}
